package com.gbroche.tpspring1.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbroche.tpspring1.model.Product;
import com.gbroche.tpspring1.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

        @Autowired
        private ProductRepository repository;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                repository.deleteAll();
        }

        @Test
        void testCreate() throws Exception {
                Product productToAdd = new Product(null, "pen", 2.5, List.of());
                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(productToAdd)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("pen"));
        }

        @Test
        void testGetById_GivenValidId_ReturnsCorrespondingProduct() throws Exception {
                Product existingProduct = repository.save(new Product(null, "pen", 2.5, List.of()));
                mockMvc.perform(get("/api/products/" + existingProduct.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("pen"));
        }

        @Test
        void testGetById_GivenInvalidId_ReturnsNotFoundResponse() throws Exception {
                mockMvc.perform(get("/api/products/1"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string("No corresponding product found"));
        }

        @Test
        void testGetAll() throws Exception {
                repository.save(new Product(null, "pen", 1.0, List.of()));
                repository.save(new Product(null, "pencil", 0.5, List.of()));
                repository.save(new Product(null, "notebook", 2.5, List.of()));

                mockMvc.perform(get("/api/products"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].name").value("pen"))
                                .andExpect(jsonPath("$[1].name").value("pencil"))
                                .andExpect(jsonPath("$[2].name").value("notebook"));
        }

        @Test
        void testUpdate() throws Exception {
                Product existingProduct = repository.save(new Product(null, "pen", 2.5, List.of()));
                Long existingProductId = existingProduct.getId();
                existingProduct.setPrice(3.0);
                mockMvc.perform(put("/api/products/" + existingProductId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(existingProduct)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(existingProductId))
                                .andExpect(jsonPath("$.price").value(3.0));
        }

        @Test
        void testDelete_GivenValidId_DeletesSuccessfully() throws Exception {
                Product existingProduct = repository.save(new Product(null, "pen", 2.5, List.of()));
                Long existingProductId = existingProduct.getId();
                mockMvc.perform(delete("/api/products/" + existingProductId))
                                .andExpect(status().isNoContent());
                assertFalse(repository.findById(existingProductId).isPresent());
        }

        @Test
        void testDelete_GivenNull_ReturnsBadRequest() throws Exception {
                mockMvc.perform(delete("/api/products/" + null))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void testDelete_GivenInvalidId_Returns404() throws Exception {
                mockMvc.perform(delete("/api/products/1"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string("No product to delete found at this id"));
        }

        @Test
        void testDuplicate_GivenValidId_CreatesCopy() throws Exception {
                Product productToCopy = repository.save(new Product(null, "pen", 2.5, List.of()));
                long productToCopyId = productToCopy.getId();
                mockMvc.perform(post("/api/products/" + productToCopyId + "/duplicate"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(productToCopyId + 1))
                                .andExpect(jsonPath("$.name").value(productToCopy.getName() + " (Copy)"));
        }

        @Test
        void testDuplicate_GivenInvalidId_Returns404() throws Exception {
                mockMvc.perform(post("/api/products/1/duplicate"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string("No product found with given ID to dupplicate"));
        }

        @Test
        void testCreateBundle_givenValidInputs_CreatesNewBundleProduct() throws Exception {
                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));
                Product p3 = repository.save(new Product(null, "notebook", 2.5, List.of()));
                Product p4 = repository.save(new Product(null, "mouse", 30.0, List.of()));

                Long expectedBundleId = p4.getId() + 1;
                String expectedBundleName = p1.getName() + "+" + p2.getName() + "+" + p3.getName();
                Double expectedBundlePrice = p1.getPrice() + p2.getPrice() + p3.getPrice();

                String requestContent = "[" + p1.getId() + "," + p2.getId() + "," + p3.getId() + "]";

                mockMvc.perform(post("/api/products/bundle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestContent))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(expectedBundleId))
                                .andExpect(jsonPath("$.name").value(expectedBundleName))
                                .andExpect(jsonPath("$.price").value(expectedBundlePrice));
        }

        @Test
        void testCreateBundle_givenLessThanTwoId_ReturnsBadRequest() throws Exception {
                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));

                mockMvc.perform(post("/api/products/bundle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[" + p1.getId() + "]"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("At least 2 products are required to create a bundle"));
        }

        @Test
        void testCreateBundle_givenAProductIdNotExisting_ReturnsBadRequest() throws Exception {
                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));

                String requestContent = "[" + p1.getId() + "," + p2.getId() + ", 1000]";

                mockMvc.perform(post("/api/products/bundle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestContent))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(
                                                "One of the product id given does not correspond to any product"));
        }

        @Test
        void testCreateBundle_givenDupplicateId_ReturnsBadRequest() throws Exception {
                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));

                Long expectedBundleIdIfCreated = p2.getId() + 1;
                String requestContent = "[" + p1.getId() + "," + p1.getId() + "," + p2.getId() + "]";

                mockMvc.perform(post("/api/products/bundle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestContent))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Duplicate product IDs are not allowed"));
                assertFalse(repository.findById(expectedBundleIdIfCreated).isPresent());
        }

        @Test
        void testCreateBundle_givenBundlesAndIdNotOverlapping_CreatesNewBundle() throws Exception {
                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));
                Product p3 = repository.save(new Product(null, "notebook", 2.5, List.of()));
                Product p4 = repository.save(new Product(null, "mouse", 30.0, List.of()));
                Product p5 = repository.save(new Product(null, "keyboard", 25.0, List.of()));

                Product[] bundle1Sources = new Product[] { p1, p2 };
                Product[] bundle2Sources = new Product[] { p4, p5 };

                Product bundle1 = repository.save(
                                new Product(
                                                null,
                                                p1.getName() + "+" + p2.getName(),
                                                p1.getPrice() + p2.getPrice(),
                                                List.of(bundle1Sources)));

                Product bundle2 = repository.save(
                                new Product(
                                                null,
                                                p4.getName() + "+" + p5.getName(),
                                                p4.getPrice() + p5.getPrice(),
                                                List.of(bundle2Sources)));

                String requestContent = "[" + p3.getId() + "," + bundle1.getId() + "," + bundle2.getId() + "]";

                Long expectedBundleId = bundle2.getId() + 1;
                String expectedBundleName = p3.getName() + "+" + bundle1.getName() + "+" + bundle2.getName();
                double expectedBundlePrice = p3.getPrice() + bundle1.getPrice() + bundle2.getPrice();

                mockMvc.perform(post("/api/products/bundle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestContent))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(expectedBundleId))
                                .andExpect(jsonPath("$.name").value(expectedBundleName))
                                .andExpect(jsonPath("$.price").value(expectedBundlePrice));
        }

        @Test
        void testCreateBundle_givenBundlesThatWouldCauseDupplicateProducts_ReturnsBadRequest() throws Exception {
                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));
                Product p3 = repository.save(new Product(null, "notebook", 2.5, List.of()));

                Product[] bundle1Sources = new Product[] { p1, p2 };
                Product[] bundle2Sources = new Product[] { p2, p3 };

                Product bundle1 = repository.save(
                                new Product(
                                                null,
                                                p1.getName() + "+" + p2.getName(),
                                                p1.getPrice() + p2.getPrice(),
                                                List.of(bundle1Sources)));

                Product bundle2 = repository.save(
                                new Product(
                                                null,
                                                p2.getName() + "+" + p3.getName(),
                                                p2.getPrice() + p3.getPrice(),
                                                List.of(bundle2Sources)));

                String requestContent = "[" + bundle1.getId() + "," + bundle2.getId() + "]";

                Long expectedBundleIdIfCreated = bundle2.getId() + 1;
                String expectedError = "The products given for the new bundle have at least one of them also being a bundle and causing dupplicate products";

                mockMvc.perform(post("/api/products/bundle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestContent))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(expectedError));
                assertFalse(repository.findById(expectedBundleIdIfCreated).isPresent());
        }

        @Test
        void testDoesBundleRequestCauseRecursion_GivenValidInput_ReturnsTrue() throws Exception {
                Method method = ProductController.class
                                .getDeclaredMethod("doesBundleRequestCauseRecursion", List.class);
                method.setAccessible(true);

                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));
                Product p3 = repository.save(new Product(null, "notebook", 2.5, List.of()));
                Product p4 = repository.save(new Product(null, "mouse", 30.0, List.of()));
                Product p5 = repository.save(new Product(null, "keyboard", 25.0, List.of()));

                Product p6 = repository.save(new Product(null, "pen+pencil+notebook", 4.0, List.of(p1, p2, p3)));
                List<Product> productsToTest = new ArrayList<>();
                productsToTest.add(p4);
                productsToTest.add(p5);
                productsToTest.add(p6);
                boolean result = (boolean) method.invoke(new ProductController(repository), productsToTest);
                assertFalse(result);
        }

        @Test
        void testDoesBundleRequestCauseRecursion_GivenInvalidInput_ReturnsFalse() throws Exception {
                Method method = ProductController.class
                                .getDeclaredMethod("doesBundleRequestCauseRecursion", List.class);
                method.setAccessible(true);

                Product p1 = repository.save(new Product(null, "pen", 1.0, List.of()));
                Product p2 = repository.save(new Product(null, "pencil", 0.5, List.of()));
                Product p3 = repository.save(new Product(null, "notebook", 2.5, List.of()));
                Product p4 = repository.save(new Product(null, "mouse", 30.0, List.of()));
                Product p5 = repository.save(new Product(null, "keyboard", 25.0, List.of()));

                Product p6 = repository.save(new Product(null, "pen+pencil+notebook", 4.0, List.of(p1, p2, p3)));
                Product p7 = repository.save(new Product(null, "notebook+mouse+keyboard", 57.5, List.of(p3, p4, p5)));
                List<Product> productsToTest = new ArrayList<>();
                productsToTest.add(p6);
                productsToTest.add(p7);
                boolean result = (boolean) method.invoke(new ProductController(repository), productsToTest);
                assertTrue(result);
        }
}
