package wang.imallen.blog.plugin.animal

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.internal.reflect.Instantiator

class Animal {

    int count

    Dog dog

    private NamedDomainObjectContainer<Cat> catContainer

    public Animal(Instantiator instantiator, NamedDomainObjectContainer<Cat> catContainer) {
        this.dog = instantiator.newInstance(Dog.class)
        this.catContainer = catContainer
    }

    //这个方法很关键，有了它才能在build.gradle文件中添加"dog"配置
    void dog(Action<Dog> action) {
        //打log会发现这个action是org.gradle.util.ConfigureUtil$1@39199237,也就是在调用configureUsing()时创建的Action的匿名内部类
        println "dog action:" + action.toString()
        action.execute(dog)
    }

    void catConfig(Action<? super NamedDomainObjectContainer<Cat>> action) {
        action.execute(catContainer)
    }

    //KP 注意:这里只能写成"\n cat info:"+catContainer而不能写成"\n cat info:"+catContainer.toString()
    @Override
    public String toString() {
        return "dog info:" + dog.toString() + "\ncat info:" + catContainer
    }

}