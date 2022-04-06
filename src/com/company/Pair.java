package com.company;

public class Pair<F,S> {
    private F first;
    private S second;



    public Pair(F first,S second){
        this.first = first;
        this.second = second;
    }

    public F getKey(){
        return first;
    }
    public S getValue(){
        return second;
    }
}
