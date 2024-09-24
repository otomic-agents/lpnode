package com.bytetrade.obridge.component;  
  
public class ChainSetting {  
    // Use the volatile keyword to ensure visibility across threads and prevent instruction reordering optimizations  
    private static volatile ChainSetting instance;  
  
    // Private constructor to prevent instantiation from outside the class  
    private ChainSetting() {  
    }  
  
    // Static method to retrieve the singleton instance of ChainSetting  
    public static ChainSetting getInstance() {  
        // First check if the instance has already been created  
        if (instance == null) {  
            // Synchronize on the ChainSetting class to ensure thread safety during instance creation  
            synchronized (ChainSetting.class) {  
                // Double-check if the instance is still null after acquiring the lock  
                if (instance == null) {  
                    instance = new ChainSetting(); // Create a new instance of ChainSetting  
                }  
            }  
        }  
        // Return the singleton instance  
        return instance;  
    }  

    public Boolean needGasSetting(Integer chainId) {
        if (chainId == 501) {
            return false;
        }
        return true;
    }
}