## 说明

RShrinker是在编译时通过扫描所有的R文件，然后将所有类中的R字段替换为常量，并且将R文件去除，以达到减少包大小的目的.


为了进行更灵活地控制，添加了rshinker这个Extension,如下是一个典型配置:

```groovy
rshrink{
    inlineR true
    skipDebugInlineR false
    skipRPkgs=["wang.imallen.blog.rshrinker.demo"]
}
```

其中3个参数的含义如下:

- inlineR:是否要进行R文件内联，如果设置为false, 则不进行内联
- skipDebugInlineR: 在debug时是否要跳过内联，设置为true表示跳过，否则不跳过。建议设置为false, 以便能尽早发现由于内联可能导致的资源找不到的问题
- skipRPkgs: 哪些包名下的R文件不进行内联