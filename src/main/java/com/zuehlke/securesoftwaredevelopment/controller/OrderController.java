package com.zuehlke.securesoftwaredevelopment.controller;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.controller.CustomerController;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.Food;
import com.zuehlke.securesoftwaredevelopment.domain.NewOrder;
import com.zuehlke.securesoftwaredevelopment.domain.User;
import com.zuehlke.securesoftwaredevelopment.repository.CustomerRepository;
import com.zuehlke.securesoftwaredevelopment.repository.OrderRepository;
import com.zuehlke.securesoftwaredevelopment.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;

@Controller
public class OrderController {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    private static final Logger LOG = LoggerFactory.getLogger(CustomerController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerController.class);

    public OrderController(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/order")
    @PreAuthorize("hasAuthority('ORDER_FOOD')")
    public String order(Model model){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();

            model.addAttribute("restaurants", customerRepository.getRestaurants());
            model.addAttribute("addresses", orderRepository.getAddresses(user.getId()));
            LOG.info("ACTION: [page for new order opened]" +
                    " USERNAME: ["+SecurityUtil.getCurrentUser().getUsername()+"]");

        return "order";
    }


    @GetMapping(value = "/api/menu", produces = "application/json")
    @ResponseBody
    @PreAuthorize("hasAuthority('ORDER_FOOD')")
    public List<Food> getMenu(@RequestParam(name="id") String id){
        int identificator = Integer.valueOf(id);
        return orderRepository.getMenu(identificator);
    }

    @PostMapping(value = "/api/new-order", consumes = "application/json")
    @ResponseBody
    @PreAuthorize("hasAuthority('ORDER_FOOD')")
    public String newOrder(@RequestBody NewOrder newOrder){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        orderRepository.insertNewOrder(newOrder, user.getId(),customerRepository);
        return "";
    }

    @ExceptionHandler(Exception.class)
    public void handleException(HttpServletRequest req, Exception ex) throws Exception {
        // prepare responseEntity
        Enumeration<String> parametri=req.getParameterNames();
        String uriParameters;
        if (parametri.hasMoreElements())
            uriParameters="?";
        else
            uriParameters="";

        while (parametri.hasMoreElements()){
            String elem=parametri.nextElement();
            uriParameters+=elem+"="+req.getParameter(elem)+" ";
        }
        LOG.warn("CLASS: ["+ex+"] USER: ["+SecurityUtil.getCurrentUser().getUsername()+"] URI: [" +req.getRequestURI()+uriParameters+"]" );
        if(ex.getClass()== AccessDeniedException.class)
            AuditLogger.getAuditLogger(CustomerController.class).audit("ACCESS DENIED! CLASS: ["+ex+"] USER: ["+SecurityUtil.getCurrentUser().getUsername()+"] URI: [" +req.getRequestURI()+uriParameters+"]");

    }
}
