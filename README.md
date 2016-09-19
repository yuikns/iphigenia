# CSE8803

Just a tiny seed of develop hadoop + spark.

## Usage

```bash
git clone --recurse-submodules git@github.com:yuikns/cse8803.git
```

It is managed by [sbt](http://www.scala-sbt.org/). You can import it into [IntelliJ](https://www.jetbrains.com/idea/).

You can add some more packages via [mvnrepository](http://mvnrepository.com/), copy and paste the path into [project/BuildCSE8803.scala](project/BuildCSE8803.scala)

After finish your developing, you can use script 

```bash
scripts/pack
```

and get .jar(in this case is target/scala-2.11/cse8803-0.0.1-2.11.8.jar, may be changed based on your change in configuration) package contains **all** the dependencies.

if you are a windows user, please type the above command instead:

```
sbt assembly
```


