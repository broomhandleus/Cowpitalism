package com.broomhandleus.maximus.cowpitalism.types;

/**
 * Class representing a single player of Cowpitalism
 */

public class Player implements Comparable<Player> {

    public String name;
    public int cows;
    public int milk;
    public int horses;
    public double money;
    public int hayBales;
    public int semis;
    public int tankers;
    public int barns;
    public boolean chickenShield;
    private int kitties;

    public Player(String newName) {
        name = newName;
        cows = 0;
        milk = 0;
        horses = 0;
        money = 0;
        hayBales = 0;
        semis = 0;
        tankers = 0;
        barns = 0;
        chickenShield = false;

        // everyone should have a kitty... or 5
        kitties = (int) (5 * Math.random());
    }

    // Makes the objects comparable for sorting
    public int compareTo(Player other) {
        if (this.cows > other.cows) {
            return 1;
        } else if (this.cows < other.cows){
            return -1;
        } else {
            return 0;
        }
    }
}
