## 系列说明

这是深入理解Gradle框架系列的第一篇。整个系列共分为9篇，文章列表如下:

- 第1篇是入门文章，主要讲解Gradle Plugin以及Extension的多种用法，以及buildSrc及gradle插件调试的方法。
- 第2篇是从dependencies出发，阐述DependencyHandler的原理
- 第3篇则是关于gradle configuration的预备知识介绍，以及artifacts的发布流程
- 第4篇则是artifacts的获取流程
- 第5篇是从TaskManager出发，分析如ApplicationTaskManager, LibraryTaskManager中各主要的Task，最后给出当前版本的编译流程图
- 第6篇比较3.2.1相比3.1.2中架构的变化
- 第7篇关于Gradle Transform
- 第8篇从打包的角度讲解app bundles的原理
- 第9篇分析资源编译的流程，特别是aapt2的编译流程



## Plugin

## 语言选择

其实只要是JVM语言，都可以用来写插件, 比如Android Gradle Plugin团队，在3.2.0之前一直是用java编写gradle插件。

然后国内很多开源项目都是用groovy编写的，groovy的优势是书写方便，而且其闭包写法非常灵活，然后groovy的缺点也非常明显，最大的一点不好就是IDE对其的支持非常不好，不仅仅是语法高亮没做好，还有导航跳转都极为有限，比如build.gradle中的方法跳转不到其定义处。

当然，我自己长期使用groovy下来，也发现了它的一些缺点，比如each这个闭包，在运行时竟然会出现找不到其成员的情况。

以及出现开发者自定义的成员与其默认成员(groovy中会为每个类增加一些默认成员)名称重合时，不能给出有效的提示，当然，这个问题我不确定是IDE的问题还是groovy自身的编译器实现不够完善的问题。

其实到目前为止，使用kotlin进行插件开发是最好的选择，有如下两个原因:

- kotlin的语法糖完全不输于groovy, 可以有效提高开发效率
- kotlin是jetbrain自家的，IDE对其的支持更完善

可能正是这个原因，Google编译工具组从3.2.0开始，新增的插件全部都是用kotlin编写的。

### 插件名与Plugin的关系

比如我们常用的apply plugin: 'com.android.application', 其实是对应的AppPlugin， 其声明在源码的META-INF中，如下图所示:

![plugin_dec](https://github.com/HiWong/GradleWiki/blob/master/Articles/images/HiWong/gradle01/plugin_dec.png)

可以看到，不仅仅有com.android.appliation, 还有我们经常用到的com.android.library,以及com.android.feature, com.android.dynamic-feature.

以com.android.application.properties为例，其内容如下:

```groovy
implementation-class=com.android.build.gradle.AppPlugin
```

其含义很清楚了，就表示com.android.application对应的插件实现类是com.android.build.gradle.AppPlugin这个类。

其他的类似，就不一一列举了。

### 定义插件的方法

要定义一个gradle plugin，则要实现Plugin接口，该接口如下:

```java
public interface Plugin<T>{
    void apply(T var)
}
```

以我们经常用的AppPlugin和LibraryPlugin, 其继承关系如下:

![plugin_arch](https://github.com/HiWong/GradleWiki/blob/master/Articles/images/HiWong/gradle01/plugin_arch.png)

注意，这是3.2.0之前的继承关系，在3.2.0之后，略微有些调整。

可以看到，LibraryPlugin和AppPlugin都继承自BasePlugin， 而BasePlugin实现了Plugin<Project>接口，如下:

```java
public abstract class BasePlugin<E extends BaseExtension2>
        implements Plugin<Project>, ToolingRegistryProvider {

    @VisibleForTesting
    public static final GradleVersion GRADLE_MIN_VERSION =
            GradleVersion.parse(SdkConstants.GRADLE_MINIMUM_VERSION);

    private BaseExtension extension;

    private VariantManager variantManager;
    
    ...
    }
```

这里继承的层级多一层的原因是，有很多共同的逻辑可以抽出来放到BasePlugin中，然而大多数时候，我们可能没有这么复杂的关系，所以直接实现Plugin<Project>这个接口即可。

## Extension

Extension其实可以理解成java中的java bean, 它的作用也是类似的，即获取输入数据，然后在插件中使用。

最简单的Extension为例, 比如我定义一个名为Student的Extension,其定义如下:

```groovy
class Student{
    String name
    int age
    boolean isMale
}
```

然后在Plugin的apply()方法中，添加这个Extension, 不然编译时会出现找不到的情形:

```groovy
project.extensions.create("student",Student.class)
```

这样，我们就可以在build.gradle中使用名为student的Extension了，如下:

```groovy
student{
    name 'Mike'
    age 18
    isMale true
}
```

注意，这个名称要与创建Extension时的名称一致。

而获取它的方式也很简单:

```groovy
Student studen = project.extensions.getByType(Student.class)
```

嵌套的Extension类似，不再赘述。

如果Extension中要包含固定数量的配置项，那很简单, 类似下面这样就可以:

```groovy
class Fruit{
    int count
    Fruit(Project project){
        project.extensions.create("apple",Apple,"apple")
        project.extension.create("banana",Banana,"banana")
    }
}
```

其配置如下:

```groovy
fruit{
    count 3
    apple{
        name 'Big Apple'
        weight 580f
    }
    
    banana{
        name 'Yellow Banana'
        size 19f
    }
}
```



下面要说的是包含不定数量的配置项的Extension, 就需要用到NamedDomainObjectContainer, 比如我们常用的编译配置中的productFlavors，就是一个典型的包含不定数量的配置项的Extension.
但是，如果我们不进行特殊处理，而是直接使用NamedDomainObjectContainer的话，就会发现这个配置项都要用=赋值，类似下面这样。

接着使用Student, 如果我需要在某个配置项中添加不定项个Student输入，其添加方式如下:

```groovy
NamedDomainObjectContainer<Student>studentContainer = project.container(Student)
project.extensions.add('team',studentContainer)
```

然而，此时其配置只能如下:

```groovy
team{
    John{
       age=18
       isMale=true
    }
    Daisy{
        age=17
        isMale=false
    }
}
```

注意，这里不需要name了，因为John和Daisy就是name了。

可是，这不科学呀，groovy的语法不是可以省略么？就比如productFlavors这样:

![productFlavors](https://github.com/HiWong/GradleWiki/blob/master/Articles/images/HiWong/gradle01/productFlavors.png)

要达到这样的效果其实并不难，只要做好以下两点:

- item Extension的定义中必须有name这个属性，因为在Factory中会在创建时为这个名称的属性赋值。定义如下:

  ```groovy
  class Cat{
      String name
      
      String from
      float weight
  }
  ```

  

- 需要定义一个实现了NamedDomainObjectFactory<T>接口的类，这个类的构造方法中必须有instantiator这个参数，如下:

  ```groovy
  class CatExtFactory implements NamedDomainObjectFactory<Cat>{
      private Instantiator instantiator
      
      CatExtFactory(Instantiator instantiator){
          this.instantiator=instantiator
      }
      
      @Override
      Cat create(String name){
          return instantiator.newInstance(Cat.class, name)
      }
  }
  ```

此时，gradle配置文件中就可以类似这样写了:

```groovy
animal{
    count 58
    
    dog{
        from 'America'
        isMale false
    }
    
    catConfig{
        chinaCat{
            from 'China'
            weight 2900.8f
        }
        
        birman{
            from 'Burma'
            weight 5600.51f
        }
        
        shangHaiCat{
            from 'Shanghai'
            weight 3900.56f
        }
        
        beijingCat{
            from 'Beijing'
            weight 4500.09f
        }
    }
}
```



## Plugin Transform

Transform是android gradle plugin团队提供给开发者使用的一个抽象类，它的作用是提供接口让开发者可以在源文件编译成为class文件之后，dex之前进行字节码层面的修改。

借助javaassist, ASM这样的字节码处理工具，可在自定义的Transform中进行代码的插入，修改，替换，甚至是新建类与方法。

像美团点评的Robust，以及我开源的[Andromeda](https://github.com/iqiyi/Andromeda)项目中，都有在Transform中插入代码的示例。

如下是一个自定义Transform实现:

```groovy
public class QigsawCompTransform extends Transform {

    private Project project;
    private IComponentProvider provider

    public QigsawCompTransform(Project project,IComponentProvider componentProvider) {
        this.project = project;
        this.provider=componentProvider
    }

    @Override
    public String getName() {
        return "QigsawCompTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        long startTime = System.currentTimeMillis();

        transformInvocation.getOutputProvider().deleteAll();
        File jarFile = transformInvocation.getOutputProvider().getContentLocation("main", getOutputTypes(), getScopes(), Format.JAR);
        if (!jarFile.getParentFile().exists()) {
            jarFile.getParentFile().mkdirs()
        }
        if (jarFile.exists()) {
            jarFile.delete();
        }

        ClassPool classPool = new ClassPool()
        project.android.bootClasspath.each{
            classPool.appendClassPath((String)it.absolutePath)
        }

        def box=ConvertUtils.toCtClasses(transformInvocation.getInputs(),classPool)

        CodeWeaver codeWeaver=new AsmWeaver(provider.getAllActivities(),provider.getAllServices(),provider.getAllReceivers())
        codeWeaver.insertCode(box,jarFile)

        System.out.println("QigsawCompTransform cost "+(System.currentTimeMillis()-startTime)+" ms")
    }
}

```



## gradle插件的发布

绝大多数gradle插件，我们可能都是只要在公司内部使用，那么只要使用公司内部的maven仓库即可，即配置并运用maven插件，然后执行其upload task即可。这个很简单，不再赘述。

## 特殊的buildSrc

前面说过gradle插件的发布，那如果我们在插件的代码编写阶段，总不能修改一点点代码，就发布一个版本，然后重新运用吧？

有人可能会说，那就不发布到maven仓库，而是发布到本地仓库呗，然而这样至多发布时节省一点点时间，仍然太麻烦。

幸好有buildSrc!

在buildSrc中定义的插件，可以直接在其他module中运用，而且是类似这种运用方式:

```groovy
apply plugin: org.qiyi.arch.comp.MainPlugin
```

即直接apply具体的类，而不是其发布名称，这样的话，不管做什么修改，都能马上体现，而不需要等到重新发布版本。

那么为什么buildSrc这个module这么特殊呢？

其实是gradle框架对它进行了特殊处理，源码如下:



## gradle插件的调试

以调试:app:assembleRelease这个task为例，其实很简单，分如下两步即可:

- 新建remote target
- 在命令行输入./gradlew --no-daemon -Dorg.gradle.debug=true :app:assembleRelease
- 之后选择刚刚创建的remote target, 然后点击调试按钮即可





















