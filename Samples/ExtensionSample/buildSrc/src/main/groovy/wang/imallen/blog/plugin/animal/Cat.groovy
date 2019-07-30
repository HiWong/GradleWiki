package wang.imallen.blog.plugin.animal

class Cat {

    //必须要有这个属性
    String name

    String from
    float weight

    public Cat(String name) {
        this.name = name
    }

    @Override
    public String toString() {
        return "name:" + name + ",from:" + from + ",weight:" + weight
    }

}
