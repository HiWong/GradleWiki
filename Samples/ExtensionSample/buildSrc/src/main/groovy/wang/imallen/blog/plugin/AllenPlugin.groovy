package wang.imallen.blog.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.reflect.Instantiator
import org.gradle.invocation.DefaultGradle
import wang.imallen.blog.plugin.animal.Animal
import wang.imallen.blog.plugin.animal.Cat
import wang.imallen.blog.plugin.animal.CatExtFactory

class AllenPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println "This is AllenPlugin"

        /////////////////////添加Animal对应的extension///////////////////////
        Instantiator instantiator = ((DefaultGradle) project.getGradle()).getServices().get(Instantiator.class)
        NamedDomainObjectContainer<Cat> catContainer = project.container(Cat.class, new CatExtFactory(instantiator))
        project.getExtensions().create("animal", Animal.class, instantiator, catContainer)

        project.task('showAnimalInfo') {
            group 'pet'
            doLast {
                Animal animal = project.extensions.getByName('animal')
                println animal.toString()
            }
        }
        /////////////////////////////////////////////////////////////////

    }
}