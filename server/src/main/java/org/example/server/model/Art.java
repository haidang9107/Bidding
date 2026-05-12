package org.example.server.model;

import java.sql.Timestamp;

public class Art extends Item {

    // =========================
    // Thuộc tính riêng
    // =========================
    private String artist;

    private String artType;

    // =========================
    // Constructor rỗng
    // =========================
    public Art() {
        super();
    }

    // =========================
    // Constructor cơ bản
    // =========================
    public Art(String productId,
               String productName,
               String description,
               double startingPrice,
               double stepPrice,
               Seller seller,
               String status,
               Timestamp createdAt) {

        super(
                productId,
                productName,
                description,
                startingPrice,
                stepPrice,
                seller,
                status,
                createdAt
        );
    }

    // =========================
    // Constructor đầy đủ
    // =========================
    public Art(String productId,
               String productName,
               String description,
               double startingPrice,
               double stepPrice,
               Seller seller,
               String status,
               Timestamp createdAt,
               String artist,
               String artType) {

        super(
                productId,
                productName,
                description,
                startingPrice,
                stepPrice,
                seller,
                status,
                createdAt
        );

        this.artist = artist;
        this.artType = artType;
    }

    // =========================
    // Getter & Setter
    // =========================

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    // -------------------------

    public String getArtType() {
        return artType;
    }

    public void setArtType(String artType) {
        this.artType = artType;
    }

    // =========================
    // Business Methods
    // =========================

    public void displayArtist() {

        System.out.println(
                "Artist: " + artist
        );
    }

    public void displayArtType() {

        System.out.println(
                "Art Type: " + artType
        );
    }

    // =========================
    // toString()
    // =========================
    @Override
    public String toString() {

        return "Art{" +
                "productId='" + getProductId() + '\'' +
                ", productName='" + getProductName() + '\'' +
                ", artist='" + artist + '\'' +
                ", artType='" + artType + '\'' +
                ", startingPrice=" + getStartingPrice() +
                ", stepPrice=" + getStepPrice() +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}