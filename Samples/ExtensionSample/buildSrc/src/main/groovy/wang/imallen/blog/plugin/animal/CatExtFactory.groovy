package wang.imallen.blog.plugin.animal

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.internal.reflect.Instantiator

class CatExtFactory implements NamedDomainObjectFactory<Cat> {

    private Instantiator instantiator

    public CatExtFactory(Instantiator instantiator) {
        this.instantiator = instantiator
    }

    @Override
    Cat create(String s) {
        return instantiator.newInstance(Cat.class, s);
    }
}