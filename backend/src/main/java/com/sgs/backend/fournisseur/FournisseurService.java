package com.sgs.backend.fournisseur;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.fournisseur.dto.FournisseurRequestDTO;
import com.sgs.backend.fournisseur.dto.FournisseurResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final CurrentUserService currentUserService;

    public List<FournisseurResponseDTO> findAll() {
        return fournisseurRepository.findByEntrepriseId(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public FournisseurResponseDTO findById(Long id) {
        return toResponseDTO(getFournisseurOrThrow(id));
    }

    public FournisseurResponseDTO create(FournisseurRequestDTO dto) {
        Fournisseur fournisseur = new Fournisseur();
        applyDto(fournisseur, dto);
        // Jamais reçue du client HTTP : déduite du JWT de l'utilisateur connecté.
        fournisseur.setEntreprise(currentUserService.getEntrepriseCourante());
        return toResponseDTO(fournisseurRepository.save(fournisseur));
    }

    public FournisseurResponseDTO update(Long id, FournisseurRequestDTO dto) {
        Fournisseur fournisseur = getFournisseurOrThrow(id);
        applyDto(fournisseur, dto);
        return toResponseDTO(fournisseurRepository.save(fournisseur));
    }

    public void delete(Long id) {
        Fournisseur fournisseur = getFournisseurOrThrow(id);
        fournisseurRepository.delete(fournisseur);
    }

    // Un fournisseur d'une autre entreprise est traité comme introuvable
    // (404), pas comme interdit (403) -- même logique que partout ailleurs (RG-10).
    private Fournisseur getFournisseurOrThrow(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = fournisseur.getEntreprise() != null
                && fournisseur.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Fournisseur introuvable avec id=" + id);
        }
        return fournisseur;
    }

    private void applyDto(Fournisseur fournisseur, FournisseurRequestDTO dto) {
        fournisseur.setNom(dto.nom());
        fournisseur.setMail(dto.mail());
        fournisseur.setNumTel(dto.numTel());
        fournisseur.setAdresse(new Adresse(dto.adresse1(), dto.adresse2(), dto.ville(), dto.codePostal(), dto.pays()));
    }

    private FournisseurResponseDTO toResponseDTO(Fournisseur fournisseur) {
        Adresse adresse = fournisseur.getAdresse();
        return new FournisseurResponseDTO(
                fournisseur.getId(),
                fournisseur.getNom(),
                adresse != null ? adresse.getAdresse1() : null,
                adresse != null ? adresse.getAdresse2() : null,
                adresse != null ? adresse.getVille() : null,
                adresse != null ? adresse.getCodePostal() : null,
                adresse != null ? adresse.getPays() : null,
                fournisseur.getMail(),
                fournisseur.getNumTel()
        );
    }
}
