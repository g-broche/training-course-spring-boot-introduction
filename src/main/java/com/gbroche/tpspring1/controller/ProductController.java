package com.gbroche.tpspring1.controller;

import com.gbroche.tpspring1.model.Product;
import com.gbroche.tpspring1.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/products")
public class ProductController {
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Product> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Product foundProduct = repository.findById(id).orElseThrow();
            return ResponseEntity.ok(foundProduct);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("No corresponding product found");
        }
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return repository.save(product);
    }

    @PostMapping(value = "/{id}/duplicate")
    public ResponseEntity<?> duplicate(@PathVariable Long id) {
        try {
            Product ProductToCopy = repository.findById(id).orElseThrow();
            Product duplicate = new Product();
            duplicate.setName(ProductToCopy.getName() + " (Copy)");
            duplicate.setPrice(ProductToCopy.getPrice());
            Product createDuplicate = repository.save(duplicate);
            return ResponseEntity.ok(createDuplicate);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("No product found with given ID to dupplicate");
        }

    }

    @PostMapping("/bundle")
    public ResponseEntity<?> createBundle(@RequestBody List<Long> productIds) {
        if (productIds.size() <= 1) {
            return ResponseEntity.badRequest().body("At least 2 products are required to create a bundle");
        }
        List<Product> baseProducts = new ArrayList<>();
        Set<Long> uniqueIds = new HashSet<>(productIds);
        if (uniqueIds.size() != productIds.size()) {
            return ResponseEntity.badRequest().body("Duplicate product IDs are not allowed");
        }
        try {
            for (Long productId : productIds) {
                Product newProductOfBundle = repository.findById(productId).orElseThrow();
                baseProducts.add(newProductOfBundle);
            }
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body("One of the product id given does not correspond to any product");
        }

        if (doesBundleRequestCauseRecursion(baseProducts)) {
            return ResponseEntity.badRequest().body(
                    "The products given for the new bundle have at least one of them also being a bundle and causing dupplicate products");
        }

        Product newBundle = new Product();

        String newBundleName = baseProducts.stream()
                .map(Product::getName)
                .collect(Collectors.joining("+"));
        Double newBundlePrice = baseProducts.stream()
                .mapToDouble(Product::getPrice)
                .sum();

        newBundle.setName(newBundleName);
        newBundle.setPrice(newBundlePrice);
        newBundle.setSources(baseProducts);

        Product savedBundle = repository.save(newBundle);
        return ResponseEntity.ok(savedBundle);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        Product existing = repository.findById(id).orElseThrow();
        existing.setName(product.getName());
        existing.setPrice(product.getPrice());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            if (!repository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(("No product to delete found at this id"));
            }
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean doesBundleRequestCauseRecursion(List<Product> products) {
        List<Long> bundleProductsId = new ArrayList<>();
        for (Product product : products) {
            bundleProductsId.add(product.getId());
            if (!product.getSources().isEmpty()) {
                for (Product source : product.getSources()) {
                    bundleProductsId.add(source.getId());
                }
            }
        }
        Set<Long> uniqueIds = new HashSet<>(bundleProductsId);
        return uniqueIds.size() != bundleProductsId.size();
    }
}