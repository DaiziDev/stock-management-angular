package com.sgs.backend.client;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.client.dto.ClientRequestDTO;
import com.sgs.backend.client.dto.ClientResponseDTO;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CurrentUserService currentUserService;

    public List<ClientResponseDTO> findAll() {
        return clientRepository.findByEntrepriseId(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ClientResponseDTO findById(Long id) {
        return toResponseDTO(getClientOrThrow(id));
    }

    public ClientResponseDTO create(ClientRequestDTO dto) {
        Client client = new Client();
        applyDto(client, dto);
        // Jamais reçue du client HTTP : déduite du JWT de l'utilisateur connecté.
        client.setEntreprise(currentUserService.getEntrepriseCourante());
        return toResponseDTO(clientRepository.save(client));
    }

    public ClientResponseDTO update(Long id, ClientRequestDTO dto) {
        Client client = getClientOrThrow(id);
        applyDto(client, dto);
        return toResponseDTO(clientRepository.save(client));
    }

    public void delete(Long id) {
        Client client = getClientOrThrow(id);
        clientRepository.delete(client);
    }

    // Un client d'une autre entreprise est traité comme introuvable (404),
    // pas comme interdit (403) -- même logique que Article/Categorie (RG-10).
    private Client getClientOrThrow(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = client.getEntreprise() != null
                && client.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Client introuvable avec id=" + id);
        }
        return client;
    }

    private void applyDto(Client client, ClientRequestDTO dto) {
        client.setNom(dto.nom());
        client.setPrenom(dto.prenom());
        client.setMail(dto.mail());
        client.setNumTel(dto.numTel());
        client.setPhoto(dto.photo());
        client.setAdresse(new Adresse(dto.adresse1(), dto.adresse2(), dto.ville(), dto.codePostal(), dto.pays()));
    }

    private ClientResponseDTO toResponseDTO(Client client) {
        Adresse adresse = client.getAdresse();
        return new ClientResponseDTO(
                client.getId(),
                client.getNom(),
                client.getPrenom(),
                adresse != null ? adresse.getAdresse1() : null,
                adresse != null ? adresse.getAdresse2() : null,
                adresse != null ? adresse.getVille() : null,
                adresse != null ? adresse.getCodePostal() : null,
                adresse != null ? adresse.getPays() : null,
                client.getMail(),
                client.getNumTel(),
                client.getPhoto()
        );
    }
}
