package jcache.web;

import java.util.ArrayList;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

@CacheDefaults(cacheName="zombies")
public class ZombiePod {
	
	private ArrayList<String> zombies = new ArrayList<String>();
	
	
	public ZombiePod() {	
		zombies.add("Alex");
		zombies.add("Mark");
		zombies.add("Andy");
		zombies.add("Jim");
		System.out.println("init - ArraySize: " + zombies.size());
	}
	
	
	@CacheResult
	public String getZombie(@CacheKey Integer z) {
		System.out.println("get - ArraySize: " + zombies.size());
		return zombies.get(z);
	}
	
	@CachePut
	public int addZombie(@CacheValue String name) {
		zombies.add(name);
		return zombies.lastIndexOf(name); 
	}
	
	@CacheRemove
	public boolean removeZombie(@CacheKey Integer z, String name) {
		if (zombies.get(z).equals(name)) {
			zombies.remove(z);
			return true;
		} else {
			//Cache is out of sync
			apocalypse();
		}
		return false;
	}
	
	@CacheRemoveAll
	public void apocalypse() {		
	}
	
	public void emptyPod() {
		zombies.clear();
	}
	

}
