package com.works.elasticsearch.elasticrepositories;

import com.works.elasticsearch.documents.ElasticCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface ECustomerRepository extends ElasticsearchRepository<ElasticCustomer,String> {

    @Query("{\"bool\":{\"must\":[],\"must_not\":[],\"should\":[{\"match\":{\"email\":\"?0\"}},{\"match\":{\"name\":\"?0\"}}]}},\"from\":0,\"size\":10,\"sort\":[],\"aggs\":{}")
    Page<ElasticCustomer> searchEmailAndName(String data, Pageable pageable);


    List<ElasticCustomer> findByEmailContainsOrCidContains(String email, String cid);


    Optional<ElasticCustomer> findByCid(Integer cid);

    void deleteByCid(Integer cid);
}
