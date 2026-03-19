package org.forecast.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${app.jwt.secret.key}")
  private String secretKey;

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
  }

  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    // Make a mutable copy of extraClaims to avoid modifying caller-provided immutable maps
    Map<String, Object> claims = new HashMap<>();
    if (extraClaims != null) claims.putAll(extraClaims);

    if (!claims.containsKey("role")) {
      userDetails.getAuthorities().stream()
          .findFirst()
          .ifPresent(
              auth -> {
                String authority = auth.getAuthority();
                if (authority != null && authority.startsWith("ROLE_")) {
                  claims.put("role", authority.substring("ROLE_".length()));
                } else {
                  claims.put("role", authority);
                }
              });
    }

    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(86400)))
        .signWith(getSignInKey())
        .compact();
  }

  public String extractRole(String token) {
    return extractClaim(
        token,
        claims -> {
          Object role = claims.get("role");
          return role == null ? null : String.valueOf(role);
        });
  }

  public String extractName(String token) {
    return extractClaim(token, claims -> claims.get("name", String.class));
  }

  public UUID extractCompanyId(String token) {
    String id = extractClaim(token, claims -> claims.get("companyId", String.class));
    if (id == null || id.isBlank()) {
      return null;
    }
    return UUID.fromString(id);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
