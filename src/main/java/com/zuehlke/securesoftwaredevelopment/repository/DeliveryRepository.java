package com.zuehlke.securesoftwaredevelopment.repository;

import com.zuehlke.securesoftwaredevelopment.config.AuditLogger;
import com.zuehlke.securesoftwaredevelopment.config.SecurityUtil;
import com.zuehlke.securesoftwaredevelopment.domain.DeliveryDetail;
import com.zuehlke.securesoftwaredevelopment.domain.ViewableDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DeliveryRepository {
    private DataSource dataSource;

    private static final Logger LOG = LoggerFactory.getLogger(CustomerRepository.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(CustomerRepository.class);

    public DeliveryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ViewableDelivery> getAllDeliveries() {
        List<ViewableDelivery> deliveries = new ArrayList<>();
        String sqlQuery = "SELECT d.id, d.isDone, d.date, d.comment, u.username, r.name, rt.name, a.name FROM delivery AS d JOIN users AS u ON d.userId = u.id JOIN restaurant as r ON d.restaurantId = r.id JOIN address AS a ON d.addressId = a.id JOIN restaurant_type AS rt ON r.typeId= rt.id";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sqlQuery)) {

            while (rs.next()) {
                deliveries.add(createDelivery(rs));
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return deliveries;
    }


    private ViewableDelivery createDelivery(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        boolean isDone = rs.getBoolean(2);
        Date date = rs.getDate(3);
        String comment = rs.getString(4);
        String username = rs.getString(5);
        String restaurantName = rs.getString(6);
        String restaurantType = rs.getString(7);
        String address = rs.getString(8);
        return new ViewableDelivery(id, isDone, date, comment, username, address, restaurantName, restaurantType);
    }

    public ViewableDelivery getDelivery(String id) {
        String sqlQuery = "SELECT d.id, d.isDone, d.date, d.comment, u.username, r.name, rt.name, a.name" +
                " FROM delivery AS d JOIN users AS u ON d.userId = u.id JOIN restaurant as r ON d.restaurantId = r.id " +
                "JOIN address AS a ON d.addressId = a.id JOIN restaurant_type AS rt ON r.typeId= rt.id WHERE d.id =?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);) {

            statement.setString(1,id);
             ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return createDelivery(rs);
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        return null;
    }

    public List<DeliveryDetail> getDeliveryDetails(String id) {
        List<DeliveryDetail> details = new ArrayList<>();
        String sqlQuery = "SELECT f.id, di.amount, f.name, f.price FROM delivery_item AS di JOIN food AS f ON di.foodId = f.id WHERE deliveryId = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);) {

            statement.setString(1,id);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                details.add(createDetail(rs));
            }

        } catch (SQLException e) {
            LOG.warn("CAUGHT -> SQLExeption CLASS: ["+e.getClass()+"] USER:["+ SecurityUtil.getCurrentUser() +"]",e);
            //e.printStackTrace();
        }
        LOG.info("ACTION: [page with delivery details opened] DELIVERY-ID: ["+id+ "] USERNAME: ["+SecurityUtil.getCurrentUser().getUsername()+"]");
        return details;
    }


    private DeliveryDetail createDetail(ResultSet rs) throws SQLException {
        int id = rs.getInt(1);
        int amount = rs.getInt(2);
        String foodName = rs.getString(3);
        int price = rs.getInt(4);

        return new DeliveryDetail(id, amount, foodName, price);

    }

    public int calculateSum(List<DeliveryDetail> details){
        int sum = 0;
        for(DeliveryDetail detail: details){
            sum+= detail.getPrice() * detail.getAmount();
        }
        return sum;
    }


    public List<ViewableDelivery> search(String searchQuery) throws SQLException {
        List<ViewableDelivery> cars = new ArrayList<>();
        String searchQuerryUpper=searchQuery.toUpperCase();
        String sqlQuery =
                "SELECT d.id, d.isDone, d.date, d.comment, u.username, r.name, rt.name, a.name FROM delivery AS d " +
                        "JOIN users AS u ON d.userId = u.id JOIN restaurant as r ON d.restaurantId = r.id " +
                        "JOIN address AS a ON d.addressId = a.id JOIN restaurant_type AS rt ON r.typeId= rt.id" +
                        " WHERE UPPER(d.comment) LIKE ?"
                        + "OR UPPER(u.username) LIKE ?"
                        + "OR UPPER(r.name) LIKE ?"
                        + "OR UPPER(rt.name) LIKE ?"
                        + "OR UPPER(a.name) LIKE ?";

        LOG.info("ACTION: [search] SEARCH-QUERY: ["+searchQuery+ "] USERNAME: ["+SecurityUtil.getCurrentUser().getUsername()+"]");


        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlQuery);) {
            statement.setString(1,"%"+searchQuerryUpper+"%");
            statement.setString(2,"%"+searchQuerryUpper+"%");
            statement.setString(3,"%"+searchQuerryUpper+"%");
            statement.setString(4,"%"+searchQuerryUpper+"%");
            statement.setString(5,"%"+searchQuerryUpper+"%");

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                cars.add(createDelivery(rs));
            }
        }
        return cars;
    }

}
