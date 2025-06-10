package com.gbroche.tpspring1.controller;

import com.gbroche.tpspring1.model.Product;
import com.gbroche.tpspring1.repository.ProductRepository;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
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
    public Product getById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return repository.save(product);
    }

    @PostMapping(value = "/{id}/duplicate")
    public Product duplicate(@PathVariable Long id) {
        Product ProductToCopy = repository.findById(id).orElseThrow();
        Product duplicate = new Product();
        duplicate.setName(ProductToCopy.getName() + " (Copy)");
        duplicate.setPrice(ProductToCopy.getPrice());
        return repository.save(duplicate);
    }

    @PostMapping("/bundle")
    public ResponseEntity createBundle(@RequestBody List<Long> productIds) {
        List<Product> baseProducts = new ArrayList<>();
        Set<Long> uniqueIds = new HashSet<>(productIds);
        if (uniqueIds.size() != productIds.size()) {
            return ResponseEntity.badRequest().body("Duplicate product IDs are not allowed");
        }
        for (Long productId : productIds) {
            Product newProductOfBundle = repository.findById(productId).orElseThrow();
            baseProducts.add(newProductOfBundle);
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
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
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