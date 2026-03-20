import { useEffect, useRef, useState } from "react";
import clsx from "clsx";

export default function Dropdown({
  trigger,
  children,
  className = "",
  menuClassName = "",
  align = "right",
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    function handleClickOutside(event) {
      if (!containerRef.current?.contains(event.target)) {
        setIsOpen(false);
      }
    }

    function handleEscape(event) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [isOpen]);

  const closeMenu = () => setIsOpen(false);
  const toggleMenu = () => setIsOpen((current) => !current);

  return (
    <div ref={containerRef} className={clsx("relative", className)}>
      {trigger({
        isOpen,
        toggle: toggleMenu,
        close: closeMenu,
      })}

      {isOpen && (
        <div
          className={clsx(
            "absolute top-full z-30 mt-1 min-w-44 overflow-hidden rounded-lg border border-slate-200 bg-white py-1 shadow-lg shadow-slate-900/10",
            align === "left" ? "left-0" : "right-0",
            menuClassName,
          )}
        >
          {typeof children === "function"
            ? children({ close: closeMenu })
            : children}
        </div>
      )}
    </div>
  );
}
