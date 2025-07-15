package com.tiendaabarrotes.elbuenprecio.Repository;

import com.tiendaabarrotes.elbuenprecio.Model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}

