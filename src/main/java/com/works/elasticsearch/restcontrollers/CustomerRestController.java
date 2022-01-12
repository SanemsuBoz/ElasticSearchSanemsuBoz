package com.works.elasticsearch.restcontrollers;

import com.works.elasticsearch.documents.ElasticCustomer;
import com.works.elasticsearch.elasticrepositories.ECustomerRepository;
import com.works.elasticsearch.entities.Customer;
import com.works.elasticsearch.repositories.CustomerRepository;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/customer")
public class CustomerRestController {
    final ElasticsearchOperations elasticsearchOperations;
    final ECustomerRepository eRepo;
    final CustomerRepository cRepo;

    public CustomerRestController(ElasticsearchOperations elasticsearchOperations, ECustomerRepository eRepo, CustomerRepository cRepo) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.eRepo = eRepo;
        this.cRepo = cRepo;
    }

    //add
    @PostMapping("/add")
    public Map<String,Object> add(@RequestBody Customer customer){
        Map<String,Object> hm=new LinkedHashMap<>();

        Customer c= cRepo.save(customer);

        //elastic insert

        ElasticCustomer ec=new ElasticCustomer();
        ec.setCid(c.getCid());
        ec.setEmail(c.getEmail());
        ec.setName(c.getName());
        eRepo.save(ec);


        hm.put("status",true);
        hm.put("result", c);

        return hm;
    }
    //list elastic
    //@GetMapping("/list/{page}")
    @GetMapping("/list")
    public Map<String,Object> list(){
        Map<String,Object> hm=new LinkedHashMap<>();

        Iterable<ElasticCustomer> ls=eRepo.findAll();

        //pageable
       // Pageable pageable=PageRequest.of(page,2);
        //Page <ElasticCustomer> lsp=eRepo.findAll(pageable);


        hm.put("status",true);
        /*hm.put("totalElements",lsp.getTotalElements());
        hm.put("totalPages",lsp.getTotalPages());
        hm.put("result", lsp.getContent());*/
        hm.put("result", ls);

        return hm;
    }

    //search and data
    @GetMapping("/search/{q}/{page}")
    public Map<String,Object> search(@PathVariable String q,@PathVariable int page){
        Map<String,Object> hm=new LinkedHashMap<>();

        Pageable pageable=PageRequest.of(page,2);
        Page <ElasticCustomer> lsp=eRepo.searchEmailAndName(q,pageable);

        hm.put("status",true);
        hm.put("totalElements",lsp.getTotalElements());
        hm.put("totalPages",lsp.getTotalPages());
        hm.put("result", lsp.getContent());

        return hm;
    }

    @GetMapping("/emailcidsearch/{q}")
    public Map<String,Object> searchEmailOrCid(@PathVariable String q){
        Map<String,Object> hm=new LinkedHashMap<>();

        List<ElasticCustomer> ls=eRepo.findByEmailContainsOrCidContains(q,q);

        hm.put("status",true);
        hm.put("result", ls);

        return hm;
    }

    @GetMapping("/globalSearch/{q}")
    public  Map<String,Object> globalSearch(@PathVariable String q){
        Map<String,Object> hm=new LinkedHashMap<>();

        final NativeSearchQuery query=new NativeSearchQueryBuilder()
                .withQuery(
                        QueryBuilders.matchQuery("name",q)
                                .fuzziness(Fuzziness.AUTO)
                                .prefixLength(2)
                                //.operator(Operator.AND) and olarak işaretlendiğinde sadece aranan ifadenin aynısını sonuç olarak gösterir
                                .minimumShouldMatch("50%") //kelimelerin aynen bulunmasını istediğimizde
                )
                .build();

        List<SearchHit<ElasticCustomer>> ls= elasticsearchOperations.search(query,ElasticCustomer.class).getSearchHits();

        hm.put("result",ls);

        return hm;
    }

    //delete ve update
   @PostMapping("/update")
    public Map<String,Object> update(@RequestBody Customer customer){
        Map<String,Object> hm=new LinkedHashMap<>();

        Optional<Customer> optC = cRepo.findCustomerByCid(customer.getCid());

       if (optC.isPresent() ) {
           Customer c = optC.get();
           c.setName(customer.getName());
           c.setTelephone(customer.getTelephone());
           c.setPassword(customer.getPassword());
           c.setEmail(customer.getEmail());
           cRepo.saveAndFlush(c);


           Optional<ElasticCustomer> optR = eRepo.findByCid(customer.getCid());
           if ( optR.isPresent() ) {
               ElasticCustomer rc = optR.get();
               rc.setName(customer.getName());
               rc.setEmail(customer.getEmail());

               eRepo.deleteById( rc.getId() );
               eRepo.save( rc );

               hm.put("status", false);
               hm.put("message", "Update ElasticSearch Action Success");
               hm.put("result", rc);

           }else {
               hm.put("status", false);
               hm.put("message", "Update ElasticSearch Action Fail");
           }

       }else {
           hm.put("status", false);
           hm.put("message", "Update DB Action Fail");
       }

       return hm;
    }

    @DeleteMapping("/delete/{cid}")
    public Map<String,Object> delete(@PathVariable String cid){

        Map<String,Object> hm=new LinkedHashMap<>();

        try {
            int cCid = Integer.parseInt(cid);

            cRepo.deleteCustomerByCid(cCid);

            Optional<ElasticCustomer> oct = eRepo.findByCid(cCid);
            if (oct.isPresent() ) {
                ElasticCustomer rc = oct.get();
                eRepo.deleteByCid(rc.getCid());
                hm.put("status", true);
                hm.put("result", rc);
            }else {
                hm.put("status", false);
                hm.put("message", "Delete Elastic Fail :" + cid);
            }

        }catch (Exception ex) {
            hm.put("status", false);
            hm.put("message", "Delete Action Fail :" + cid);
        }
        return hm;
    }


}
