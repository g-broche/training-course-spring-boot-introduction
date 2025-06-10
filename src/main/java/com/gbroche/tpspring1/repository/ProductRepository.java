package com.gbroche.tpspring1.repository;

import com.gbroche.tpspring1.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
