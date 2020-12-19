package com.demo.domain.service;

import java.math.BigDecimal;

/**
 * 创建微服务时，需要创建一个高内聚、低耦合的微服务。而DDD中的限界上下文则完美匹配微服务要求，可以将该限界上下文理解为一个微服务进程。
 * <p>
 * 1。贫血模型
 * 2。事务脚本
 * 3。基于领域对象
 * <p>
 * 如何确定聚合跟？
 * Order中的item(orderItems)和总价(totalPrice)是密切相关的，orderItems的变化会直接导致totalPrice的变化，因此，这二者自然应该内聚在Order下。
 * <p>
 * 如何判断业务逻辑什么时候该放在领域模型中，什么时候放在领域服务中？可以从以下几点考虑：
 * <p>
 * 不是属于单个聚合根的业务或者需要多个聚合根配合的业务，放在领域服务中，注意是业务，如果没有业务，协调工作应该放到应用服务中
 * 静态方法放在领域服务中
 * 需要通过rpc等其它外部服务处理业务的，放在领域服务中
 */
public class TestDomainService {

    /**
     * 1。贫血模型（贫血的领域对象：Order仅用作数据载体，而没有行为和动作的领域对象）
     * 这种方式依然时一种面向过程的编程范式，不是OO原则。
     * <p>
     * 导致时指责划分不清晰，本应该内聚在Order中的业务，都放在service中整合，导致Order只是充当了一个数据容器的贫血模型。
     * <p>
     * 可能会出现新的问题：在项目演化的过程中，业务都在domainService和appService中，对之后的代码扩展能力会减弱。
     */
    public void changeProductCount1(Long id, ChangeProductCountCommand command) {
        Order order = DAO.findById(id);
        if (order.getStatus() == OrderStatus.PAID) {
            throw new OrderCannotBeModifiedException(id);
        }
        //面向过程：根据商品id查询商品详情，setcount，settotalprice，调用Dao保存
        Order orderItem = order.getOrderItem(command.getProductId());
        orderItem.setCount(command.getCount());
        order.setTotalPrice(calculateTotalPrice(order));
        DAO.saveOrUpdate(order);
    }

    /**
     * 2。事务脚本（没有提出领域对象，像脚本执行是的拼装业务）
     * 领域对象Order存在的意义就是为了包装mapper，不使用orm的情况下毫无意义。
     * <p>
     * 注：在系统业务逻辑极度简单的情况下，确实是个非常好的实践。但是度不是很好把握。。。
     * <p>
     * 业务逻辑分散：DAO封装出了多个能力，摒弃了面向领域对象处理业务的思想，用一行行脚本的形式去实现业务。
     * <p>
     * 可能会出现新的问题：业务不停发展，这种模式对日后的影响比贫血模型可怕的多。
     */
    public void changeProductCount2(Long id, ChangeProductCountCommand command) {
        OrderStatus orderStatus = DAO.getOrderStatus(id);
        if (orderStatus == OrderStatus.PAID) {
            throw new OrderCannotBeModifiedException(id);
        }
        DAO.updateProductCount(id, command.getProductId(), command.getCount());
        DAO.updateTotalPrice(id);
    }


    /**
     * 3。基于领域对象
     * “检查Order状态”、“修改Product数量”以及“更新Order总价” 这些正是Order应该具有的职责。
     */
    public void changeProductCount3(Long productId, int count) {
        Order order = DAO.byId(productId);
        if (order.status == OrderStatus.PAID) {
            throw new OrderCannotBeModifiedException(productId);
        }
        order.changeProductCount(count);
        order.updateTotalPrice();
        DAO.saveOrUpdate(order);
    }

    /**
     * 领域对象-Order
     */
    public class Order {
        Long id;
        Long count;
        BigDecimal totalPrice;
        OrderStatus status;


        private void updateTotalPrice() {

        }

        public Order getOrderItem(Long productId) {
            // todo 获取Order对象item
            return null;
        }

        /**
         * 更新product-count
         */
        private void updateCount(int count) {
            //todo
        }

        /**
         * 检索order-item
         */
        private Order retrieveItem(Long productId) {
            //todo
            return null;
        }


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }


        public void changeProductCount(int count) {
        }
    }

    public class ChangeProductCountCommand {
        private Long productId;
        private Long count;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    public enum OrderStatus {
        PAID
    }


    public class OrderCannotBeModifiedException extends RuntimeException {
        public OrderCannotBeModifiedException(Long id) {

        }
    }

    /**
     * 计算价格
     */
    private BigDecimal calculateTotalPrice(Order order) {
        return null;
    }

    /**
     * dao接口（方便模拟）
     */
    public static class DAO {
        public static Order findById(Long id) {
            return null;
        }

        public static void saveOrUpdate(Order order) {

        }

        public static OrderStatus getOrderStatus(Long id) {
            return null;
        }

        public static void updateProductCount(Long id, Long productId, Long count) {

        }

        public static void updateTotalPrice(Long id) {

        }

        public static Order byId(Long productId) {
            return null;
        }
    }

}
