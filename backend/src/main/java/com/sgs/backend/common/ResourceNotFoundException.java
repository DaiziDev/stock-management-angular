package com.sgs.backend.common;

// Exception "métier" : on la lève quand on cherche une ressource par id
// et qu'elle n'existe pas. On l'attrape plus haut dans GlobalExceptionHandler
// pour la transformer en réponse HTTP 404 propre, au lieu de laisser
// une stacktrace brute remonter au client.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
