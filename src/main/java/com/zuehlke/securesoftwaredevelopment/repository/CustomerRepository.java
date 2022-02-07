package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.security.AccessControlException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerRepository.class);

    private DataSource dataSource;

    public CustomerRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Person createPersonFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String firstName = rs.getString(2);
        String lastName = rs.getString(3);
        String personalNumber = rs.getString(4);
        String address = rs.getString(5);
        return new Person(id, firstName, lastName, personalNumber, address);
    }

    public List<Customer> getCustomers() {
        List<com.zuehlke.securesoftwaredevelopment.domain.Customer> customers = new ArrayList<com.zuehlke.securesoftwaredevelopment.domain.Customer>();
        String query = "SELECT id, username FROM users";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                customers.add(createCustomer(rs));
            }

            //throw new AccessControlException("lala");

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return customers;
    }

    private com.zuehlke.securesoftwaredevelopment.domain.Customer createCustomer(ResultSet rs) throws SQLException {
        return new com.zuehlke.securesoftwaredevelopment.domain.Customer(rs.getInt(1), rs.getString(2));
    }

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<Restaurant>();
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id ";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {

            while (rs.next()) {
                restaurants.add(createRestaurant(rs));
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            // e.printStackTrace();
        }
        return restaurants;
    }

    private Restaurant createRestaurant(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        String address = rs.getString(3);
        String type = rs.getString(4);

        return new Restaurant(id, name, address, type);
    }


    public Object getRestaurant(String id) {
        String query = "SELECT r.id, r.name, r.address, rt.name  FROM restaurant AS r JOIN restaurant_type AS rt ON r.typeId = rt.id WHERE r.id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);){

             statement.setString(1,id);
             ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                LOG.info("ACTION: [page with restaurant details opened] RESTAURANT-ID: ["+id+ "] " +
                        "USERNAME: ["+SecurityUtil.getCurrentUser().getUsername()+"]");
                return createRestaurant(rs);
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return null;
    }

    public void deleteRestaurant(int id) {
        String restorantName= ((Restaurant) getRestaurant(""+id)).getName();

        System.out.println("ID "+id );
        String query = "DELETE FROM restaurant WHERE id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(query);
            auditLogger.audit("ACTION:[restaurant deleted] DELETED RESTAURANT NAME:["+restorantName+"] RESTAURANT ID:["+id+"] BY:["+SecurityUtil.getCurrentUser().getUsername()+"]");

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            // e.printStackTrace();
        }
    }

    private String getRestaurantType(int id){

        String query = "SELECT name FROM restaurant_type WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setInt(1,id);
            ResultSet res=statement.executeQuery();
            if(res.next()){
                return res.getString("name");
            }
        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return "";
    }

    public void updateRestaurant(RestaurantUpdate restaurantUpdate) {
        Restaurant restaurant= (Restaurant) getRestaurant(""+restaurantUpdate.getId());

        String query = "UPDATE restaurant SET name = ?, address=?, typeId =? WHERE id =?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
             statement.setString(1,restaurantUpdate.getName());
             statement.setString(2,restaurantUpdate.getAddress());
             statement.setInt(3,restaurantUpdate.getRestaurantType());
             statement.setInt(4,restaurantUpdate.getId());

            statement.executeUpdate();
            auditLogger.audit("ACTION:[restaurant updated] OLD NAME:["+restaurant.getName()+"]" +
                    " NEW NAME:["+restaurantUpdate.getName()+"]" +
                    " OLD ADDRESS:["+restaurant.getAddress()+"] " +
                    " NEW ADDRESS: ["+restaurantUpdate.getAddress()+"]" +
                    " OLD TYPE:["+restaurant.getRestaurantType()+"]" +
                    " NEW TYPE:["+getRestaurantType(restaurantUpdate.getRestaurantType())+"] BY:["+SecurityUtil.getCurrentUser().getUsername()+"]");


        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }

    public Customer getCustomer(String id) {

        String sqlQuery = "SELECT id, username, password FROM users WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);) {

             statement.setString(1,id);
             ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                LOG.info("ACTION: [page with customer details opened] CUSTOMER-ID: ["+id+ "] " +
                        "USERNAME: ["+SecurityUtil.getCurrentUser().getUsername()+"]");
                return createCustomerWithPassword(rs);
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return null;
    }

    private Customer createCustomerWithPassword(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String username = rs.getString(2);
        String password = rs.getString(3);
        return new Customer(id, username, password);
    }

    public String getUserUsername(String id){
        String userUsername=this.getCustomer(id).getUsername();
        return userUsername;
    }

    public void deleteCustomer(String id) {
        String deletedUsername=getUserUsername(id);
        String query = "DELETE FROM users WHERE id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(query);
            auditLogger.audit("ACTION: [user deleted] DELETED USER: ["+deletedUsername+"] BY: ["+SecurityUtil.getCurrentUser().getUsername()+"]");
        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }

    public void updateCustomer(CustomerUpdate customerUpdate) {
        Customer oldCustomerData=getCustomer(""+customerUpdate.getId());
        String query = "UPDATE users SET username = ?, password=? WHERE id =?" ;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1,customerUpdate.getUsername());
            statement.setString(2,customerUpdate.getPassword());
            statement.setInt(3,customerUpdate.getId());

            statement.executeUpdate();
            if(oldCustomerData.getPassword().equals(customerUpdate.getPassword()))
            auditLogger.audit("ACTION: [user updated] OLD USERNAME: ["+oldCustomerData.getUsername()+"] NEW USERNAME: ["+customerUpdate.getUsername()+"] BY: ["+SecurityUtil.getCurrentUser().getUsername()+"]");
            else
            auditLogger.audit("ACTION: [user updated + password changed] OLD USERNAME: ["+oldCustomerData.getUsername()+"] NEW USERNAME: ["+customerUpdate.getUsername()+"] BY: ["+SecurityUtil.getCurrentUser().getUsername()+"]");

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }

    public List<Address> getAddresses(String id) {

        String sqlQuery = "SELECT id, name FROM address WHERE userId=?";
        List<Address> addresses = new ArrayList<Address>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);) {

            statement.setString(1,id);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                addresses.add(createAddress(rs));
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return addresses;
    }

    private Address createAddress(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        return new Address(id, name);
    }

    public String getAdressById(int id){

        String query = "SELECT name FROM address WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setInt(1,id);
            ResultSet res=statement.executeQuery();
            if(res.next()){
                return res.getString("name");
            }
        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return "";
    }

    public void deleteCustomerAddress(int id) {
        String adr=getAdressById(id);
        String query = "DELETE FROM address WHERE id=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(query);
            auditLogger.audit("ACTION:[address deleted] DELETED ADDRESS:["+adr+"] ADDRESS ID:["+id+"] BY:["+SecurityUtil.getCurrentUser().getUsername()+"]");

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }

    public void updateCustomerAddress(Address address) {
        String adr=getAdressById(address.getId());
        String query = "UPDATE address SET name = ? WHERE id =?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1,address.getName());
            statement.setInt(2,address.getId());

            statement.executeUpdate();
            auditLogger.audit("ACTION:[address updated] OLD ADDRESS:["+adr+"] NEW ADDRESS:["+address.getName()+"] ADDRESS ID:["+address.getId()+"] BY:["+SecurityUtil.getCurrentUser().getUsername()+"]");

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }

    public void putCustomerAddress(NewAddress newAddress) {
        String query = "INSERT INTO address (name, userId) VALUES (?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1,newAddress.getName());
            statement.setInt(2,newAddress.getUserId());

            statement.executeUpdate();
            auditLogger.audit("ACTION:[new address] NEW ADDRESS:["+newAddress.getName()+"] BY:["+SecurityUtil.getCurrentUser().getUsername()+"]");

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }
}
