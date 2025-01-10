package com.bytetrade.obridge.db;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SwapOrderRepository extends MongoRepository<SwapOrder, String> {

}
