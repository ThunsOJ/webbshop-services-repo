package com.example.ordersservice.controllers;


import ErrorHandler.ErrorResponse;
import com.example.ordersservice.dto.OrderDTO;
import com.example.ordersservice.exceptions.CustomerNotFoundException;
import com.example.ordersservice.models.Customer;
import com.example.ordersservice.models.Orders;
import com.example.ordersservice.models.Product;
import com.example.ordersservice.repositories.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/order")
@ResponseStatus(code = HttpStatus.CREATED)
public class OrderController {


    private final OrderRepository orderRepository;
    private RestTemplate restTemplate;


    @Value("${customer-service.url}")
    private String customerServiceBaseUrl;

    @Value("${product-service.url}")
    private String productServiceBaseUrl;

    @Autowired
    public OrderController(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;

    }




    @GetMapping("/all")
    public ResponseEntity<Iterable<Orders>> all(){
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/test")
    public ResponseEntity<Customer> test() {
        Customer customer = restTemplate.getForObject("http://webbshop-services-repo-customer-service-1:8080/customer/1", Customer.class);
        if (customer == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(customer);
    }

    @GetMapping(path = "/test2")
    public @ResponseBody Customer getCustomer() {
        return restTemplate.getForObject("http://webbshop-services-repo-customer-service-1:8080/customer/1", Customer.class);
    }

    @GetMapping("/allWithPojo")
    public ResponseEntity<?> allWithPojo() {
        return ResponseEntity.ok(orderRepository.findAll().stream().map(e-> {
            Customer customer = restTemplate.getForObject(customerServiceBaseUrl + e.getCustomerId(), Customer.class);
            HttpEntity<List<Long>> requestEntity = new HttpEntity<>(e.getProductIds());
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    productServiceBaseUrl + "/list",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<Product> products = response.getBody();
            if (customer != null){
                if(products != null){
                    if(products.size() == e.getProductIds().size()){
                        return OrderDTO.builder()
                                .id(e.getId())
                                .customer(customer)
                                .products(products)
                                .build();
                    }else {
                        return "OrderWithMessageDTo med ordern + melindas objekt med en lista av de IDS som inte finns" +
                                "e.getProductIds().stream().map(ee -> {" +
                                "if(!products.getId().contains(ee)){" +
                                "return ee" +
                                "}" +
                                "}).toList()";
                    }
                } else {
                    return ResponseEntity.ok(new ErrorResponse().getTimestamp().getHour()).getStatusCode().getClass();
                }

            } else {
                return ResponseEntity.ok(new ErrorResponse());
            }

        }).toList());
    }

    @GetMapping("/all/{userId}")
    public ResponseEntity<?> allByUser(@PathVariable Long userId) {

        Customer customer = restTemplate.getForObject(customerServiceBaseUrl + userId, Customer.class);
        if(customer != null){
            return ResponseEntity.ok(orderRepository.findAllByCustomerId(customer.getId()).stream().map(e-> {
                HttpEntity<List<Long>> requestEntity = new HttpEntity<>(e.getProductIds());
                ResponseEntity<List<Product>> response = restTemplate.exchange(
                        productServiceBaseUrl + "/list",
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<>() {
                        }
                );
                List<Product> products = response.getBody();
                if(products != null){
                    if(products.size() == e.getProductIds().size()){
                        return OrderDTO.builder()
                                .id(e.getId())
                                .customer(customer)
                                .products(products)
                                .build();
                    }else {
                        return "OrderWithMessageDTo med ordern + melindas objekt med en lista av de IDS som inte finns" +
                                "e.getProductIds().stream().map(ee -> {" +
                                "if(!products.getId().contains(ee)){" +
                                "return ee" +
                                "}" +
                                "}).toList()";
                    }
                } else {
                    return ResponseEntity.ok(new ErrorResponse());
                }
            }).toList());
        } else {
            return ResponseEntity.ok(new ErrorResponse());
        }
    }

    @PostMapping("add/{customerId}")

    public ResponseEntity<?> addOrder(@PathVariable Long customerId,
                                      @RequestBody List<Long> productsIds){

        if(restTemplate.getForObject(customerServiceBaseUrl + customerId, Customer.class) != null){
            HttpEntity<List<Long>> requestEntity = new HttpEntity<>(productsIds);
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    productServiceBaseUrl + "/list",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<Product> products = response.getBody();
            if (products != null){
                if(products.size() == productsIds.size()){
                    return ResponseEntity.ok(orderRepository.save(Orders.builder()
                                    .customerId(customerId)
                                    .productIds(productsIds)
                            .build()));
                }else{
                 throw new CustomerNotFoundException(customerId);
                }
            } else {
                return ResponseEntity.ok(new ErrorResponse());
            }
        } else {
            return ResponseEntity.ok(new ErrorResponse());

        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> byOrderId(@PathVariable Long orderId) {

        Orders order = orderRepository.findById(orderId).orElseThrow();
        Customer customer = restTemplate.getForObject(customerServiceBaseUrl + order.getCustomerId(), Customer.class);
        if (customer != null){
            HttpEntity<List<Long>> requestEntity = new HttpEntity<>(order.getProductIds());
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    productServiceBaseUrl + "/list",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<Product> products = response.getBody();
            if(products != null){
                if(products.size() == order.getProductIds().size()){
                    return ResponseEntity.ok(OrderDTO.builder()
                            .id(order.getId())
                            .customer(customer)
                            .products(products)
                            .build());
                } else {
                    throw new CustomerNotFoundException(customer.getId());
                }
            }else{
                return ResponseEntity.ok(new ErrorResponse());
            }
        }else{
            return ResponseEntity.ok(new ErrorResponse());
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ CustomerNotFoundException.class })
    public ErrorResponse handleCustomerNotFoundException(CustomerNotFoundException exception) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(exception.getMessage());
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND);
        return error;
    }
}
