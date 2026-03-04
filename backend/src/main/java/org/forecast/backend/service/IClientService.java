package org.forecast.backend.service;

import org.forecast.backend.dtos.CreateClientRequest;
import org.forecast.backend.model.Client;

import java.util.UUID;

public interface IClientService {

    Client create(CreateClientRequest request);

    Client get(UUID clientId);
}
