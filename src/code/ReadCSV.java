/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package code;

public class ReadCSV {
    
    String tra_id;
    String cus_id;
    String pro_id;
    String pro_name;
    String qty;
    String per_unit;
    String date;
    String total_price;
    String region;

    public ReadCSV(String tra_id, String cus_id, String pro_id, String pro_name, String qty, String per_unit, String date, String total_price, String region) {
        this.tra_id = tra_id;
        this.cus_id = cus_id;
        this.pro_id = pro_id;
        this.pro_name = pro_name;
        this.qty = qty;
        this.per_unit = per_unit;
        this.date = date;
        this.total_price = total_price;
        this.region = region;
    }

    public String getTra_id() {
        return tra_id;
    }

    public void setTra_id(String tra_id) {
        this.tra_id = tra_id;
    }

    public String getCus_id() {
        return cus_id;
    }

    public void setCus_id(String cus_id) {
        this.cus_id = cus_id;
    }

    public String getPro_id() {
        return pro_id;
    }

    public void setPro_id(String pro_id) {
        this.pro_id = pro_id;
    }

    public String getPro_name() {
        return pro_name;
    }

    public void setPro_name(String pro_name) {
        this.pro_name = pro_name;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getPer_unit() {
        return per_unit;
    }

    public void setPer_unit(String per_unit) {
        this.per_unit = per_unit;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTotal_price() {
        return total_price;
    }

    public void setTotal_price(String total_price) {
        this.total_price = total_price;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
    
    
    
    
}
