package com.sgs.backend.categorie;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// JpaRepository<Categorie, Long> te donne GRATUITEMENT, sans écrire une ligne de SQL :
// save(), findById(), findAll(), deleteById(), existsById(), etc.
// Spring Data JPA génère l'implémentation au démarrage, à partir du nom de l'interface.
// Le <Categorie, Long> = <type de l'entité, type de sa clé primaire>.
public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    // Ici on peut ajouter des méthodes "magiques" : Spring Data comprend le nom
    // de la méthode et génère la requête tout seul, à partir des noms de champs.
    boolean existsByCode(String code);

    List<Categorie> findByEntrepriseId(Long entrepriseId);
}
