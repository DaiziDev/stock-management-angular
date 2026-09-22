package com.sgs.backend.categorie;

import com.sgs.backend.categorie.dto.CategorieRequestDTO;
import com.sgs.backend.categorie.dto.CategorieResponseDTO;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// @Service : cette classe contient la LOGIQUE MÉTIER. C'est elle qui décide
// des règles ("un code de catégorie doit être unique"), pas le Controller
// (qui ne fait que router la requête HTTP) ni le Repository (qui ne fait
// que parler à la base de données).
//
// @RequiredArgsConstructor (Lombok) génère un constructeur avec tous les
// champs "final" -> c'est comme ça qu'on fait de l'injection de dépendances
// par constructeur, la manière recommandée avec Spring (plutôt que @Autowired
// sur le champ directement).
@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final CurrentUserService currentUserService;

    public List<CategorieResponseDTO> findAll() {
        return categorieRepository.findByEntrepriseId(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CategorieResponseDTO findById(Long id) {
        return toResponseDTO(getCategorieOrThrow(id));
    }

    public CategorieResponseDTO create(CategorieRequestDTO dto) {
        if (categorieRepository.existsByCode(dto.code())) {
            // Règle métier simple : pas deux catégories avec le même code.
            // C'est le genre de règle qui n'a rien à faire dans le Controller.
            throw new IllegalArgumentException("Une catégorie avec le code '" + dto.code() + "' existe déjà");
        }
        Categorie categorie = new Categorie();
        categorie.setCode(dto.code());
        categorie.setDesignation(dto.designation());
        // Jamais reçue du client : déduite du JWT de l'utilisateur connecté.
        categorie.setEntreprise(currentUserService.getEntrepriseCourante());
        Categorie saved = categorieRepository.save(categorie);
        return toResponseDTO(saved);
    }

    public CategorieResponseDTO update(Long id, CategorieRequestDTO dto) {
        Categorie categorie = getCategorieOrThrow(id);
        categorie.setCode(dto.code());
        categorie.setDesignation(dto.designation());
        Categorie saved = categorieRepository.save(categorie);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        Categorie categorie = getCategorieOrThrow(id);
        categorieRepository.delete(categorie);
    }

    // Même logique que ArticleService.getArticleOrThrow : une catégorie
    // d'une autre entreprise est traitée comme introuvable (404), pas
    // comme interdite (403), pour ne rien révéler à un tenant sur les
    // données d'un autre (RG-10).
    private Categorie getCategorieOrThrow(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = categorie.getEntreprise() != null
                && categorie.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Catégorie introuvable avec id=" + id);
        }
        return categorie;
    }

    // Petite méthode privée de mapping Entité -> DTO. Sur un projet plus gros
    // on utiliserait MapStruct pour générer ce mapping, mais à la main
    // c'est plus simple à comprendre pour l'instant.
    private CategorieResponseDTO toResponseDTO(Categorie categorie) {
        return new CategorieResponseDTO(categorie.getId(), categorie.getCode(), categorie.getDesignation());
    }
}
