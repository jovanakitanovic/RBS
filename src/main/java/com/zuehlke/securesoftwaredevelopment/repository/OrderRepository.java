package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private DataSource dataSource;
    private static final Logger LOG = LoggerFactory.getLogger(HashedUserRepository.class);

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public List<Food> getMenu(int id) {
        List<Food> menu = new ArrayList<>();
        String sqlQuery = "SELECT id, name FROM food WHERE restaurantId=" + id;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sqlQuery)) {
            while (rs.next()) {
                menu.add(createFood(rs));
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }

        return menu;
    }

    private Food createFood(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        String name = rs.getString(2);
        return new Food(id, name);
    }

    public void insertNewOrder(NewOrder newOrder, int userId,CustomerRepository customerRepository) {
        System.out.println("ADRESA za dostavu "+newOrder.getAddress());
        LocalDate date = LocalDate.now();
        String sqlQuery = "INSERT INTO delivery (isDone, userId, restaurantId, addressId, date, comment)" +
                "values (?,?,?,?,?,?)";

        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement statementPrepared = connection.prepareStatement(sqlQuery);

            statementPrepared.setBoolean(1,false);
            statementPrepared.setInt(2,userId);
            statementPrepared.setInt(3,newOrder.getRestaurantId());
            statementPrepared.setInt(4,newOrder.getAddress());
            statementPrepared.setString(5, ""+date.getYear() + "-" + date.getMonthValue() + "-" + date.getDayOfMonth());
            statementPrepared.setString(6,newOrder.getComment());
            statementPrepared.executeUpdate();

            Statement statement = connection.createStatement();
            sqlQuery = "SELECT MAX(id) FROM delivery";
            ResultSet rs = statement.executeQuery(sqlQuery);

            if (rs.next()) {

                int deliveryId = rs.getInt(1);
                sqlQuery = "INSERT INTO delivery_item (amount, foodId, deliveryId)" +
                        "values";
                for (int i = 0; i < newOrder.getItems().length; i++) {
                    FoodItem item = newOrder.getItems()[i];
                    String deliveryItem = "";
                    if (i > 0) {
                        deliveryItem = ",";
                    }
                    deliveryItem += "(" + item.getAmount() + ", " + item.getFoodId() + ", " + deliveryId + ")";
;
                    sqlQuery += deliveryItem;
                }

                statement.executeUpdate(sqlQuery);

                String orderItems="";
                List<Food> meni=this.getMenu(newOrder.getRestaurantId());
                for (int i=0;i<newOrder.getItems().length;i++)
                    for (int j=0;j<meni.size();j++)
                        if(meni.get(j).getId()==newOrder.getItems()[i].getFoodId())
                            orderItems+="( foodid: "+meni.get(j).getName()+", food name "+meni.get(j).getId()+", amount: "+newOrder.getItems()[i].getAmount()+")";

                Restaurant res= (Restaurant) customerRepository.getRestaurant(""+newOrder.getRestaurantId());

                AuditLogger.getAuditLogger(OrderRepository.class).audit("ACTION [new order added] " +
                        "ORDER: [restaurant id: "+newOrder.getRestaurantId()+
                        " restaurant: "+ res.getName()+
                        " food: ["+orderItems+"] comment:"+newOrder.getComment()+"]" +
                        "ADDRESS:["+customerRepository.getAdressById(newOrder.getAddress())+"]"+
                        " USER: ["+SecurityUtil.getCurrentUser().getUsername()+"]");
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
    }

    public Object getAddresses(int userId) {
        List<Address> addresses = new ArrayList<>();
        //System.out.println("adresa "+userId);
        String sqlQuery = "SELECT id, name FROM address WHERE userId=" + userId;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sqlQuery)) {
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
}
