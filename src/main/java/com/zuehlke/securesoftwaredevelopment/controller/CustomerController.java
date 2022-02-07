package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;

import com.zuehlke.securesoftwaredevelopment.config.CSRFTokenMissMatch;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import com.zuehlke.securesoftwaredevelopment.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.swing.text.Utilities;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

@Controller

public class CustomerController {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerController.class);

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers-and-restaurants")
    public String customersAndRestaurants(Model model) {
        if (SecurityUtil.hasPermission("USERS_LIST_VIEW")) {
            model.addAttribute("customers", customerRepository.getCustomers());
        }
        if (SecurityUtil.hasPermission("RESTAURANT_LIST_VIEW")) {
            model.addAttribute("restaurants", customerRepository.getRestaurants());
        }
        return "customers-and-restaurants";
    }

    @GetMapping("/customer")
    @PreAuthorize("hasAuthority('USERS_DETAILS_VIEW')")
    public String getCustomer(@RequestParam(name = "id", required = true) String id, Model model, HttpSession session) {
        //String csrf=session.getAttribute("CSRF_TOKEN").toString();
        System.out.println(session.getAttribute("CSRF_TOKEN"));
        model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
        model.addAttribute("customer", customerRepository.getCustomer(id));
        model.addAttribute("addresses", customerRepository.getAddresses(id));
        return "customer";
    }

    @DeleteMapping("/customer")
    @PreAuthorize("hasAuthority('USERS_DELETE')")
    public String deleteCustomer(@RequestParam(name = "id", required = true) String id) {
        customerRepository.deleteCustomer(id);
        return "customers-and-restaurants";
    }

    @PostMapping("/api/customer/update-customer")
    @PreAuthorize("hasAuthority('USERS_EDIT')")
    public String updateCustomer(CustomerUpdate customerUpdate, Model model, HttpSession session,
                                 @RequestParam("csrfToken") String csrfToken) throws CSRFTokenMissMatch {
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
       if (!csrf.equals(csrfToken)) {
            throw new CSRFTokenMissMatch("Forbidden");
        } else {
            customerRepository.updateCustomer(customerUpdate);
            customersAndRestaurants(model);
       }
        return "customers-and-restaurants";
    }

    @DeleteMapping("/customer/address")
    @PreAuthorize("hasAuthority('USERS_EDIT')")
    public String deleteCustomerAddress(@RequestParam(name = "id", required = true) String id) {
        int identificator = Integer.valueOf(id);
        customerRepository.deleteCustomerAddress(identificator);
        return "customers-and-restaurants";
    }

    @PostMapping("/api/customer/address/update-address")
    @PreAuthorize("hasAuthority('USERS_EDIT')")
    public String updateCustomerAddress(Address address, Model model) {
        customerRepository.updateCustomerAddress(address);
        customersAndRestaurants(model);
        return "customers-and-restaurants";
    }

    @PostMapping("/customer/address")
    @PreAuthorize("hasAuthority('USERS_EDIT')")
    public String putCustomerAddress(NewAddress newAddress, Model model) {
        customerRepository.putCustomerAddress(newAddress);
        customersAndRestaurants(model);
        return "customers-and-restaurants";
    }

    @GetMapping("/restaurant")
    @PreAuthorize("hasAuthority('RESTAURANT_DETAILS_VIEW')")
    public String getRestaurant(@RequestParam(name = "id", required = true) String id, Model model) {
        model.addAttribute("restaurant", customerRepository.getRestaurant(id));
        return "restaurant";
    }

    @DeleteMapping("/restaurant")
    @PreAuthorize("hasAuthority('RESTAURANT_DELETE')")
    public String deleteRestaurant(@RequestParam(name = "id", required = true) String id) {
        int identificator = Integer.valueOf(id);
        customerRepository.deleteRestaurant(identificator);
        return "customers-and-restaurants";
    }

    @PostMapping("/api/restaurant/update-restaurant")
    @PreAuthorize("hasAuthority('RESTAURANT_EDIT')")
    public String updateRestaurant(RestaurantUpdate restaurantUpdate, Model model) {
        customerRepository.updateRestaurant(restaurantUpdate);
        customersAndRestaurants(model);
        return "customers-and-restaurants";
    }

    @ExceptionHandler(Exception.class)
    public void handleException(HttpServletRequest req, Exception ex) throws Exception {
        // prepare responseEntity
        Enumeration<String> parametri = req.getParameterNames();
        String uriParameters;
        if (parametri.hasMoreElements())
            uriParameters = "?";
        else
            uriParameters = "";

        while (parametri.hasMoreElements()) {
            String elem = parametri.nextElement();
            uriParameters += elem + "=" + req.getParameter(elem) + " ";
        }
        LOG.warn("CLASS: [" + ex.getClass() + "] USER: [" + SecurityUtil.getCurrentUser().getUsername() + "] URI:" +
                " [" + req.getRequestURI() + uriParameters + "]");
        if (ex.getClass() == AccessDeniedException.class)
            AuditLogger.getAuditLogger(CustomerController.class).audit("ACCESS DENIED! CLASS: [" + ex + "] " +
                    "USER: [" + SecurityUtil.getCurrentUser().getUsername() + "] " +
                    "URI: [" + req.getRequestURI() + uriParameters + "]");

        if (ex.getClass() == MissingServletRequestParameterException.class || ex.getClass()==CSRFTokenMissMatch.class)
            AuditLogger.getAuditLogger(CustomerController.class).audit("CSRF ATEMPTED! CLASS: [" + ex + "] " +
                    "USER: [" + SecurityUtil.getCurrentUser().getUsername() + "] " +
                    "URI: [" + req.getRequestURI() + uriParameters + "]");
    }

}
