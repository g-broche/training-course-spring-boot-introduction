# training-course-spring-boot-introduction
Training course brief introducing Spring Boot framework for handling API request.

## Environment

This project is made using Spring Boot.

To start taking in API requests use the command `mvnw spring-boot:run` from the project root and it will listen for request at http://localhost:8080/, .

As this is just intended as a test application for discovering Spring Boot, the database used will be H2.
CRUD operations required for the task involved getting the list of products, creating a new product, updating an existing product, deleting a product, creating a duplicate of a product and creating a new producting by bundling existing products. 
This bundle case also involved preventing a bundle to feature a same product several times either through the initial list of products' id provided or through attempting to include bundles with overlapping content products id. 

Test on ProductController are implement through MockMvc to simulate requests on existing enpoints and asserting if the results is conform to expectations.

As required by the brief, a set of Curl requests is also provided below to test the entire scope of CRUD operations that were intended for this brief.

## curl query set

* Create product :

`curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Pen\",\"price\":2.5}" http://localhost:8080/api/products` 

* List products :

`curl http://localhost:8080/api/products`

* Display product by id :

`curl http://localhost:8080/api/products/1`

* Update existing product :

`curl -X PUT -H "Content-Type: application/json" -d "{\"name\":\"Blue pen\",\"price\":2.7}" http://localhost:8080/api/products/1`

* Delete product:

`curl -X DELETE http://localhost:8080/api/products/1`

* Duplicate existing product (replace id by product to copy)
    * First add a new product to dupplicate

    `curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Pen\",\"price\":2.5}" http://localhost:8080/api/products`

    * Dupplicate new product (If requests not done in a row, replace id "2" by a valid product id)

    `curl -X POST http://localhost:8080/api/products/2/duplicate`

* Creating bundles:
    * Add base products:

    `curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Keyboard\",\"price\":30.00}" http://localhost:8080/api/products`

    `curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Mouse\",\"price\":25.00}" http://localhost:8080/api/products`

    `curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Monitor\",\"price\":100.00}" http://localhost:8080/api/products`

    `curl -X POST -H "Content-Type: application/json" -d "{\"name\":\"Headset\",\"price\":90.00}" http://localhost:8080/api/products`

    * Create initial bundle using "Keyboard", "Mouse" and "Monitor" (replace id if not following query sequence from start):

    `curl -X POST http://localhost:8080/api/products/bundle -H "Content-Type: application/json" -d "[4,5,6]"`

    * Error for dupplicates id if attempting to create a bundle with two "Headset"

    `curl -X POST http://localhost:8080/api/products/bundle -H "Content-Type: application/json" -d "[7,7]"`

    * Error for attempting to create a bundle using a product itself being a bundle and a product that is part of that existing bundle. For this we will use the bundle previously created with "Keyboard", "Mouse" and "Monitor" which is id 8 if the sequence of queries is followed.

    `curl -X POST http://localhost:8080/api/products/bundle -H "Content-Type: application/json" -d "[4,8]"`