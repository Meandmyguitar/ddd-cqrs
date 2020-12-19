```txt
《以下文字目录》
1. 落地用户界面com.demo.web.controller
2. 落地应用服务com.demo.application.command
3. 落地领域模型com.demo.domain.aggregate
4. 落地领域服务com.demo.domain.service
5. 落地基础设施com.demo.infrastructure
6. 落地查询服务com.demo.application.query
7. 落地MQ、Event、Cache等
8. 落地RPC和防腐层
```


# 落地用户界面com.demo.web.controller
Controller作为六边形架构中与HTTP端口的适配器，起到了适配请求，委托应用服务处理的任务。对称性架构的好处就在于，当增加新的用户的界面时我们可以创建一个新包去承载适配器(比如为暴露RPC服务创建com.demo.remoting包)，然后调用应用层的服务。这里我们有一个规范：所有查询的条件封装成XXXQry对象，所有命令的请求封装成XXXCommand对象。
```java

package com.demo.web.controller;

@RestController
@RequestMapping("/cargo")
public class CargoController {

    @Autowired
    CargoQueryService cargoQueryService;
    @Autowired
    CargoCmdService cargoCmdService;


    @RequestMapping(value = "/{cargoId}", method = RequestMethod.GET)
    public CargoDTO cargo(@PathVariable String cargoId) {
        return cargoQueryService.getCargo(cargoId);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void book(@RequestBody CargoBookCommand cargoBookCommand) {
        cargoCmdService.bookCargo(cargoBookCommand);
    }

    @RequestMapping(value = "/{cargoId}/delivery", method = RequestMethod.PUT)
    public void modifydestinationLocationCode(@PathVariable String cargoId,
            @RequestBody CargoDeliveryUpdateCommand cmd) {
        cmd.setCargoId(cargoId);
        cargoCmdService.updateCargoDelivery(cmd);
    }

}
```
我们考虑校验逻辑应该放到哪一层的时候确定这一层代码可以有请求参数的基本校验，但是 应用服务的校验逻辑是必须存在的，校验和应用服务的耦合是紧密的。

# 应用服务com.demo.application.command

com.demo.application.command包里面是具体CommandService的抽象和实现，它将协调领域模型和领域服务完成业务功能，此处不包含任何逻辑。我们认为应用服务的每个方法与用例是一一对应的，典型的处理流程是：

校验
协调领域模型或者领域服务
持久化
发布领域事件
在这一层可以使用流程编排，典型的流程也可以使用技术手段固化，比如抽象模板模式。
```java
package com.demo.application.command.impl;

@Service
public class CargoCmdServiceImpl implements CargoCmdService {

    @Autowired
    private CargoRepository cargoRepository;
    @Autowired
    DomainEventPublisher domainEventPublisher;

    @Override
    public void bookCargo(CargoBookCommand cargoBookCommand) {
        // create Cargo
        DeliverySpecification delivery = new DeliverySpecification(
                cargoBookCommand.getOriginLocationCode(),
                cargoBookCommand.getDestinationLocationCode());

        Cargo cargo = Cargo.newCargo(CargoDomainService.nextCargoId(), cargoBookCommand.getSenderPhone(),
                cargoBookCommand.getDescription(), delivery);

        // saveCargo
        cargoRepository.save(cargo);
        
        // post domain event
        domainEventPublisher.publish(new CargoBookDomainEvent(cargo));
    }

    @Override
    public void updateCargoDelivery(CargoDeliveryUpdateCommand cmd) {
        // validate

        // find
        Cargo cargo = cargoRepository.find(cmd.getCargoId());

        // domain logic
        cargo.changeDelivery(cmd.getDestinationLocationCode());

        // save
        cargoRepository.save(cargo);
    }

}
```

我们再看看应用服务的代码发现，发布领域事件的动作放在了应用层没有放在领域层，而领域事件的定义是在领域层(紧接着会提到)，这是为什么呢？如果 不考虑持久化，发布领域事件的确应该在领域模型中，但是在代码落地时，考虑到持久化完成后才代表有了真实的事件，所以我们将触发事件的代码放到了资源库后面。

# 落地领域模型com.demo.domain.aggregate
我们采用了aggregate而不是model，是为了将聚合根的概念显现出来，每个聚合根单独成一个子包，在单个聚合根中包含所需要的 值对象、领域事件的定义、资源库的抽象接口等，这里解释下为什么这些对象会在领域模型中，因为它们更能体现这个领域模型，而且资源库的抽象和聚合根有着对应的关系(不大于聚合根的数量)。
```java
package com.demo.domain.aggregate.cargo;

import com.demo.domain.aggregate.cargo.valueobject.DeliverySpecification;

public class Cargo {

    private String id;
    private String senderPhone;
    private String description;
    private DeliverySpecification delivery;

    public Cargo(String id) {
        this.id = id;
    }

    public Cargo() {}

    /**
     * Factory method：预订新的货物
     * 
     * @param senderPhone
     * @param description
     * @param delivery
     * @return
     */
    public static Cargo newCargo(String id, String senderPhone, String description,
            DeliverySpecification delivery) {
        Cargo cargo = new Cargo(id);
        cargo.senderPhone = senderPhone;
        cargo.description = description;
        cargo.delivery = delivery;
        return cargo;
    }


    public void changeDelivery(String destinationLocationCode) {
        if (this.delivery
                .getOriginLocationCode().equals(destinationLocationCode)) { throw new IllegalArgumentException(
                        "destination and origin location cannot be the same."); }
        this.delivery.setDestinationLocationCode(destinationLocationCode);
    }

    public void changeSender(String senderPhone) {
        this.senderPhone = senderPhone;
    }

}
```
特别提醒的是，聚合根对象的创建不应该被Spring容器管理，也不应该被注入其它对象。我们注意到聚合根对象可以通过静态工厂方法Factory Method来创建，下文还会介绍如何落地资源库Repository进行聚合根的创建。

# 领域服务com.demo.domain.service

很多朋友无法判断业务逻辑什么时候该放在领域模型中，什么时候放在领域服务中，可以从以下几点考虑：

不是属于单个聚合根的业务或者需要多个聚合根配合的业务，放在领域服务中，注意是业务，如果没有业务，协调工作应该放到应用服务中
静态方法放在领域服务中
需要通过rpc等其它外部服务处理业务的，放在领域服务中
```java
package com.demo.domain.service;

@Service
public class CargoDomainService {

    public static final int MAX_CARGO_LIMIT = 10;
    public static final String PREFIX_ID = "CARGO-NO-";

    /**
     * 货物物流id生成规则
     * 
     * @return
     */
    public static String nextCargoId() {
        return PREFIX_ID + (10000 + new Random().nextInt(9999));
    }

    public void updateCargoSender(Cargo cargo, String senderPhone, HandlingEvent latestEvent) {

        if (null != latestEvent
                && !latestEvent.canModifyCargo()) { throw new IllegalArgumentException(
                        "Sender cannot be changed after RECIEVER Status."); }

        cargo.changeSender(senderPhone);
    }

}
```

# 落地基础设施com.demo.infrastructure
基础设施可以对抽象的接口进行实现，上文中说到资源库Repository的接口定义在领域层，那么在基础设施中就可以具体实现这个接口。
```java
package com.demo.infrastructure.db.repository;

@Component
public class CargoRepositoryImpl implements CargoRepository {

    @Autowired
    private CargoMapper cargoMapper;

    @Override
    public Cargo find(String id) {
        CargoDO cargoDO = cargoMapper.select(id);
        Cargo cargo = CargoConverter.deserialize(cargoDO);
        return cargo;
    }

    @Override
    public void save(Cargo cargo) {
        CargoDO cargoDO = CargoConverter.serialize(cargo);
        CargoDO data = cargoMapper.select(cargoDO.getId());
        if (null == data) {
            cargoMapper.save(cargoDO);
        } else {
            cargoMapper.update(cargoDO);
        }
    }

}
```

资源库Repository的实现就是将聚合根对象持久化，往往聚合根的定义和数据库中定义的结构并不一致，我们将数据库的对象称为数据对象DO，当持久化时就需要将聚合根 序列化 成数据库数据对象，通过资源库获取(构造)聚合根时，也需要 反序列化 数据库数据对象。

我们可以基于反射或其它技术手段完成序列化和反序列化操作，这样可以避免聚合根中编写过多的getter和setter方法。

# 落地查询服务com.demo.application.query

application应用服务包含了commond和query两个子包，其实query可以提取出去和application平级，但是这两种做法没有对错，只是选择问题。

CQRS中查询服务不会调用应用服务，也不会调用领域模型和资源库Repository，它会直接查询数据库或者ES获取原始数据对象DO，然后组装成数据传输对象DTO给用户界面，这个组装的过程称为Assembler，由于与用户界面有一定的对应关系，所以Assembler放在查询服务中。

那么问题来了，查询服务中如何获取DO呢？通常的做法是在查询模块中定义抽象接口，由基础设施实现从数据库获取数据 ，但是DO的定义不是在基础设施层吗，查询服务怎么可以访问到这些对象呢？我们有两个办法：

查询服务中定义一套一摸一样的DO，然后基础设施做转换，缺点是有点复杂，冗余了DO，优点是完美符合DIP原则：抽象在查询服务中，实现在基础设施。
将DO放到shared Data & Service中去，这样就只要一套DO供查询服务和命令服务使用，查询服务定义接口，基础设施实现接口

具体落地我们发现方法1太复杂了，方法2和mybatis结合会产生疑惑，因为mybatis Mapper就是一个接口，何须在查询服务中再定义一套接口呢？最终落地的代码在查询服务和DB交互时 破坏了DIP原则，直接依赖Mapper读取数据对象进行组装。
我们来看看一个查询服务的实现，其中CargoDTOAssembler是一个组装器：
```java
package com.demo.application.query.impl;

@Service
public class CargoQueryServiceImpl implements CargoQueryService {

    @Autowired
    private CargoMapper cargoMapper;
    
    @Autowired
    private CargoDTOAssembler converter;

    @Override
    public List<CargoDTO> queryCargos() {
        List<CargoDO> cargos = cargoMapper.selectAll();
        return cargos.stream().map(converter::apply).collect(Collectors.toList());
    }

    @Override
    public List<CargoDTO> queryCargos(CargoFindbyCustomerQry qry) {
        List<CargoDO> cargos = cargoMapper.selectByCustomer(qry.getCustomerPhone());
        return cargos.stream().map(converter::apply).collect(Collectors.toList());
    }

    @Override
    public CargoDTO getCargo(String cargoId) {
        CargoDO select = cargoMapper.select(cargoId);
        return converter.apply(select);
    }
}
```

是否需要将每个对象都转化成DTO返回给用户界面这个要看情况，个人认为当DO能满足界面需求时是可以直接返回DO数据的。

# 落地MQ、Event、Cache等

毫无疑问，MQ、Event、Cache的实现都应该在基础设施层，它们接口的定义放在哪里呢？一个方案是哪一层使用了抽象就在那一层定义接口，另一个方案是放到一个共有的抽象包下，基础设施和对应层依赖这个共有的抽象包。

最终落地我选择将这些接口放在了com.demo.shared包下，这个包的定义就是共有的数据和抽象。
我们以领域事件为例来看看代码如何实现，首先定义抽象接口com.demo.shared.DomainEventPublisher：

package com.demo.shared;

public interface DomainEventPublisher {
public void publish(Object event);
}

然后在基础实施中实现，具体实现采用guava的Eventbus方案：
package com.demo.infrastructure.event;

@Component
public class GuavaDomainEventPublisher implements DomainEventPublisher {

    @Autowired
    EventBus eventBus;

    public void publish(Object event) {
        eventBus.post(event);
    }

}

发送事件的代码已经落地，那么监听事件的代码应该如何落地了呢？同样的，监听MQ的代码如何落地呢？按照架构图的指导，这些 监听器都应该充当着适配器的作用，所以它们的落地都应该放在基础设施层。
我们来看看具体监听器的实现：
package com.demo.infrastructure.event.comsumer;

@Component
public class CargoListener {

    @Autowired
    private CargoCmdService cargoCmdService;
    @Autowired
    private EventBus eventBus;
    
    @PostConstruct
    public void init(){
        eventBus.register(this);
    }

    @Subscribe
    public void recordCargoBook(CargoBookDomainEvent event) {
        // invoke application service or domain service
    }
}

监听器的基本流程就是适配领域事件，然后调用应用服务去处理。

落地RPC和防腐层

前面提到过，当我们暴露一个RPC服务时和web层是平等对待的，比如暴露一个dubbo协议的服务就和暴露一个http的服务是平等的。这一小节我们将来探讨如何与第三方系统的RPC服务进行交互。

这里涉及到DDD中Bounded Context和Context Map的概念，在领域驱动设计中，限界上下文之间是不能直接交互的，它们需要通过Context Map进行交互，在微服务足够细致的年代，我们可以做到一个微服务就代表着一个限界上下文。

当我们与第三方系统RPC交互时，就要考虑如何设计Context Map，典型的模式有Shared Kernel共享内核模式和Anti-corruption防腐层模式，最终落地时我们选择了防腐层模式，它的结构如下图(图来自《实现领域驱动设计》一书)所示：



图片: https://uploader.shimo.im/f/Weo9HQs4HmG4IJru.png



图中Adapter就是适配器，通用做法会再创建一个Translator实现上下文模型之间的翻译功能。
在看具体代码落地前还有一个问题需要强调，其它限界上下文的模型在我们系统中并不是一个模型实体，而是一个值对象，很显然Adapter应该放在基础设施层中，那么这个值对象存放在哪里呢？
我们可以将值对象和抽象接口定义在领域层，然后基础设施通过适配器和翻译器实现抽象接口，很明显这个做法是非常可取的。在具体落地时我们发现，这些值对象可能同时又被查询服务依赖，所以值对象和抽象接口定义在shared Data & Service中也是可取的，具体放在那里因看法而异。
接下来我们来看看适配器的基本实现，其中RemoteServiceTranslator起到了模型之间翻译的作用。
package com.demo.infrastructure.rpc.salessystem;
@Component
public class RemoteServiceAdapter {

    @Autowired
    private RemoteServiceTranslator translator;

    // @Autowired
    // remoteService

    public UserDO getUser(String phone) {
        // User user = remoteService.getUser(phone);
        // return this.translator.toUserDO(user);
        return null;
    }

    public EnterpriseSegment deriveEnterpriseSegment(Cargo cargo) {
        // remote service
        // translator
        return EnterpriseSegment.FRUIT;
    }

}
