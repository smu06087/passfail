package com.passfail.battle.util;

import java.util.HashMap;
import java.util.Map;

public class Mulberry32 {
    private long state;
    
    private static Map<Long, Mulberry32> instances = new HashMap<>(); 

    private Mulberry32(long seed) {
        this.state = seed;
    }
    
    public static Mulberry32 getInstance(long seed) {return getInstance(seed, false);}
    public static Mulberry32 getInstance(long seed, boolean resetSeed)
    {
    	Mulberry32 mulberryInstance = null;	
    	if(instances.containsKey(seed))
    	{
    		mulberryInstance = instances.get(seed);
    		if(resetSeed)
    		{    
    			instances.remove(seed);
    			mulberryInstance = null;
    			mulberryInstance = new Mulberry32(seed);
        		instances.put(seed,mulberryInstance);
    		}
    	}
    	else
    	{
    		mulberryInstance = new Mulberry32(seed);
    		instances.put(seed,mulberryInstance);
    	}
    	
    	return mulberryInstance;
    }

    public double getRandom() {
        state += 0x6D2B79F5L;
        long t = state;
        t = (t ^ (t >>> 15)) * (t | 1L);
        t ^= t + (t ^ (t >>> 7)) * (t | 61L);
        long result = (t ^ (t >>> 14)) >>> 0;
        return (double) (result & 0xFFFFFFFFL) / 4294967296.0;
    }

    // JS의 Math.floor(random() * range)와 동일한 기능
    public int getRandomInt(int min, int max) {
        return (int) Math.floor(getRandom() * (max - min)) + min;
    }
}