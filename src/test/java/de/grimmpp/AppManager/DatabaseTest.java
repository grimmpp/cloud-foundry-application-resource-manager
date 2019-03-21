package de.grimmpp.AppManager;

import com.zaxxer.hikari.HikariDataSource;
import de.grimmpp.AppManager.model.database.*;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { AppManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class DatabaseTest {

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private BindingRepository bindingRepository;

    @Value("${spring.jpa.database-platform}")
    private String dbPlatform;

    @Autowired
    DataSource dataSource;

    @Test
    public void datasourceTest() {
        Assert.assertEquals("org.hibernate.dialect.H2Dialect", dbPlatform);

        Assert.assertNotNull(dataSource);

        Assert.assertTrue(dataSource instanceof HikariDataSource);
        HikariDataSource hds = (HikariDataSource)dataSource;

        Assert.assertEquals("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1",hds.getJdbcUrl());
        Assert.assertEquals("org.h2.Driver",hds.getDriverClassName());
        Assert.assertEquals("HikariPool-1",hds.getPoolName());
        Assert.assertEquals("sa",hds.getUsername());
        Assert.assertEquals("sa",hds.getPassword());
    }

    @Test
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void ServiceInstanceAndParamterTest() {
        cleanDatabase();

        String id = UUID.randomUUID().toString();
        String planId = UUID.randomUUID().toString();
        String orgId = UUID.randomUUID().toString();
        String spaceId = UUID.randomUUID().toString();
        String time = "1w 3d 5m";

        Map<String,Object> parameters = new HashMap<>();
        parameters.put("time", time);

        serviceInstanceRepository.save(
                ServiceInstance.builder()
                .serviceInstanceId(id)
                .orgId(orgId)
                .spaceId(spaceId)
                .servicePlanId(planId)
                .build());

        List<ServiceInstance> siList = Lists.newArrayList(serviceInstanceRepository.findAll());
        Assert.assertEquals(1, siList.size());
        Assert.assertEquals(id, siList.get(0).getServiceInstanceId());
        Assert.assertEquals(planId, siList.get(0).getServicePlanId());
        Assert.assertEquals(spaceId, siList.get(0).getSpaceId());
        Assert.assertEquals(orgId, siList.get(0).getOrgId());

        parameterRepository.save(
                Parameter.builder()
                .reference(id)
                .key("time")
                .value(time)
                .build());
        parameterRepository.saveAll(Parameter.convert("_"+id, parameters));

        List<Parameter> parameterList = Lists.newArrayList(parameterRepository.findAll());
        Assert.assertEquals(2, parameterList.size());

        parameterList = parameterRepository.findParametersByReference(id);
        Assert.assertEquals(1, parameterList.size());
    }

    @Test
    public void BindingTest() {
        cleanDatabase();

        String serviceInstanceId = UUID.randomUUID().toString();
        String appId = UUID.randomUUID().toString();
        String bindingId = UUID.randomUUID().toString();

        Binding b = Binding.builder()
                .applicationId(appId)
                .bindingId(bindingId)
                .serviceInstanceId(serviceInstanceId)
                .build();

        bindingRepository.save(b);
        Binding b2 = bindingRepository.findById(bindingId).get();
        Assert.assertNotNull(b2);
        Assert.assertEquals(bindingId, b2.getBindingId());
        Assert.assertEquals(appId, b2.getApplicationId());
        Assert.assertEquals(serviceInstanceId, b2.getServiceInstanceId());
    }

    public void cleanDatabase() {
        serviceInstanceRepository.deleteAll();
        bindingRepository.deleteAll();
        parameterRepository.deleteAll();
    }
}