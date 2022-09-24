package main.server;

import main.protocol.Domain;

public class Annonce {
    private String owner; // on the server side its the maiil but when annonce are sent to client it is replace with the username
    private String title;
    private String content;
    private Domain dom;
    private int price;
    private int id;

    public Annonce(String owner, Domain dom, String title, String content, int price, int id) {
        this.owner = owner;
        this.dom = dom;
        this.title = title;
        this.content = content;
        this.price = price;
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    @Override
    public String toString() {
        return "(" + this.owner + ", " + this.title + ", " + this.content + ", " + this.price + ", " + this.id + ")";
    }

    public String getOwner() {
        return this.owner;
    }

    public int getId() {
        return this.id;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Domain getDomain() {
        return this.dom;
    }
    
    public String getContent() {
        return this.content;
    }
    
    public int getPrice() {
        return this.price;
    }

    public void setDescriptif(String descriptif) {
        this.content = descriptif;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Annonce))
            return false;
        Annonce c = (Annonce) o;
        return this.owner.equals(c.getOwner()) && this.title.equalsIgnoreCase(c.getTitle()) && this.content.equals(c.getContent()) && this.dom.equals(c.getDomain()) && this.price == c.getPrice() && this.id == c.getId();
    }
}
