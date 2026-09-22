package com.sgs.backend.entreprise;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import com.sgs.backend.entreprise.dto.EntrepriseUpdateDTO;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepository utilisateurRepository;

    public List<EntrepriseResponseDTO> findAll() {
        return entrepriseRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public EntrepriseResponseDTO findById(Long id) {
        return toResponseDTO(getOrThrow(id));
    }

    public EntrepriseResponseDTO create(EntrepriseRequestDTO dto) {
        if (entrepriseRepository.existsByNom(dto.nom())) {
            throw new IllegalArgumentException("Une entreprise avec le nom '" + dto.nom() + "' existe déjà");
        }
        Entreprise entreprise = new Entreprise();
        applyDto(entreprise, dto);
        return toResponseDTO(entrepriseRepository.save(entreprise));
    }

    /**
     * Mise à jour des coordonnées (SANS le nom : identifiant de
     * cloisonnement RG-10, non modifiable depuis l'API d'édition).
     */
    public EntrepriseResponseDTO update(Long id, EntrepriseUpdateDTO dto) {
        Entreprise entreprise = getOrThrow(id);
        entreprise.setAdresse(new Adresse(
                dto.adresse1(), dto.adresse2(), dto.ville(), dto.codePostal(), dto.pays()
        ));
        entreprise.setMail(dto.mail());
        entreprise.setNumTel(dto.numTel());
        return toResponseDTO(entrepriseRepository.save(entreprise));
    }

    public void delete(Long id) {
        if (!entrepriseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entreprise introuvable avec id=" + id);
        }
        entrepriseRepository.deleteById(id);
    }

    private Entreprise getOrThrow(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise introuvable avec id=" + id));
    }

    private void applyDto(Entreprise entreprise, EntrepriseRequestDTO dto) {
        entreprise.setNom(dto.nom());
        entreprise.setMail(dto.mail());
        entreprise.setNumTel(dto.numTel());
        entreprise.setAdresse(new Adresse(dto.adresse1(), dto.adresse2(), dto.ville(), dto.codePostal(), dto.pays()));
    }

    private EntrepriseResponseDTO toResponseDTO(Entreprise entreprise) {
        Adresse adresse = entreprise.getAdresse();
        return new EntrepriseResponseDTO(
                entreprise.getId(),
                entreprise.getNom(),
                adresse != null ? adresse.getAdresse1() : null,
                adresse != null ? adresse.getAdresse2() : null,
                adresse != null ? adresse.getVille() : null,
                adresse != null ? adresse.getCodePostal() : null,
                adresse != null ? adresse.getPays() : null,
                entreprise.getMail(),
                entreprise.getNumTel(),
                utilisateurRepository.countByEntrepriseId(entreprise.getId())
        );
    }
}
