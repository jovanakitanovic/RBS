package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.domain.RestaurantUpdate;
import com.zuehlke.securesoftwaredevelopment.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller

public class RestaurantController {

  /*  private static final Logger LOG = LoggerFactory.getLogger(RestaurantController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(RestaurantController.class);

    private final CustomerRepository customerRepository;

    public RestaurantController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/all-restaurants")
    @PreAuthorize("hasAuthority('USERS_LIST_VIEW')")
    public String customersAndRestaurants(Model model) {
        //model.addAttribute("customers", customerRepository.getCustomers());
        model.addAttribute("restaurants", customerRepository.getRestaurants());
        return "all-restaurants";
    }

    @GetMapping("/restaurant")
    public String getRestaurant(@RequestParam(name = "id", required = true) String id, Model model) {
        model.addAttribute("restaurant", customerRepository.getRestaurant(id));
        return "restaurant";
    }

    @DeleteMapping("/restaurant")
    public String deleteRestaurant(@RequestParam(name = "id", required = true) String id) {
        int identificator = Integer.valueOf(id);
        customerRepository.deleteRestaurant(identificator);
        return "customers-and-restaurants";
    }

    @PostMapping("/api/restaurant/update-restaurant")
    public String updateRestaurant(RestaurantUpdate restaurantUpdate, Model model) {
        customerRepository.updateRestaurant(restaurantUpdate);
        customersAndRestaurants(model);
        return "customers-and-restaurants";

    }*/
}
