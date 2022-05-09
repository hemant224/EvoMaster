package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import java.io.File
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

object GeneSamplerForTests {

    val geneClasses: List<KClass<out Gene>> = loadAllGeneClasses()

    private fun loadAllGeneClasses(): List<KClass<out Gene>> {

        val genes = mutableListOf<KClass<out Gene>>()
        /*
                    Load all the classes that extends from Gene
                 */
        val target = File("target/classes")
        if (!target.exists()) {
            throw IllegalStateException("Compiled class folder does not exist: ${target.absolutePath}")
        }

        target.walk()
                .filter { it.name.endsWith(".class") }
                .map {
                    val s = it.path.replace("\\", "/")
                            .replace("target/classes/", "")
                            .replace("/", ".")
                    s.substring(0, s.length - ".class".length)
                }
                .filter { !it.endsWith("\$Companion") }
                .filter { !it.contains("$") }
                .forEach {
                    //println("Analyzing $it")
                    val c = try {
                        this.javaClass.classLoader.loadClass(it).kotlin
                    } catch (e: Exception) {
                        println("Failed to load class: ${e.message}")
                        throw e
                    }
                    val subclass: Boolean = try {
                        Gene::class.isSuperclassOf(c)
                    } catch (e: java.lang.UnsupportedOperationException) {
                        false
                    }
                    if (subclass) {
                        genes.add(c as KClass<out Gene>)
                    }
                }
        return genes
    }


    fun <T> sample(klass: KClass<T>, rand: Randomness): T where T : Gene {

        return when (klass) {
            /*
                Note that here we do NOT randomize the values of genes, but rather
                the (fixed) constraints
             */
            StringGene::class -> sampleStringGene(rand) as T
            ArrayGene::class -> sampleArrayGene(rand) as T
            Base64StringGene::class -> sampleBase64StringGene(rand) as T
            BigDecimalGene::class -> sampleBigDecimalGene(rand) as T
            BooleanGene::class -> sampleBooleanGene(rand) as T
            CycleObjectGene::class -> sampleCycleObjectGene(rand) as T
            DisruptiveGene::class -> sampleDisruptiveGene(rand) as T
            DoubleGene::class -> sampleDoubleGene(rand) as T
            EnumGene::class -> sampleEnumGene(rand) as T
            ImmutableDataHolderGene::class -> sampleImmutableDataHolderGene(rand) as T
            IntegerGene::class -> sampleIntegerGene(rand) as T
            LimitObjectGene::class -> sampleLimitObjectGene(rand) as T
            LongGene::class -> sampleLongGene(rand) as T
            MapGene::class -> sampleMapGene(rand) as T
            NumericStringGene::class -> sampleNumericStringGene(rand) as T
            ObjectGene::class -> sampleObjectGene(rand) as T
            OptionalGene::class -> sampleOptionalGene(rand) as T
            else -> throw IllegalStateException("No sampler for $klass")

            //TODO need for all Genes
            // when genes need input genes, we sample those at random as well
        }
    }



    fun sampleOptionalGene(rand: Randomness) : OptionalGene{

        val selection = geneClasses

        return OptionalGene(
                name="rand OptionalGene",
                gene=sample(rand.choose(selection), rand)
        )
    }

    fun sampleObjectGene(rand: Randomness) : ObjectGene{

        val selection = geneClasses

        return ObjectGene(
                name = "rand ObjectGene ${rand.nextInt()}",
                fields = listOf(
                        sample(rand.choose(selection), rand),
                        sample(rand.choose(selection), rand),
                        sample(rand.choose(selection), rand)
                )
        )
    }

    fun sampleNumericStringGene(rand: Randomness) : NumericStringGene{
        return NumericStringGene(
                name= "rand NumericStringGene",
                minLength = rand.nextInt(2),
                number = sample(BigDecimalGene::class, rand)
        )
    }

    fun sampleMapGene(rand: Randomness): MapGene<*,*>{

        val min = rand.nextInt(0,2)

        return MapGene(
                name = "rand MapGene",
                minSize = rand.choose(listOf(null, min)),
                maxSize = rand.choose(listOf(null, min + rand.nextInt(1,3))),
                template = sample(PairGene::class, rand)
        )
    }

    fun sampleLimitObjectGene(rand: Randomness): LimitObjectGene{
        return LimitObjectGene(name="rand LimitObjectGene")
    }

    fun sampleImmutableDataHolderGene(rand: Randomness): ImmutableDataHolderGene{
        return ImmutableDataHolderGene(
                name="rand ImmutableDataHolderGene",
                value = rand.nextWordString(),
                inQuotes = rand.nextBoolean()
        )
    }

    fun sampleEnumGene(rand: Randomness) : EnumGene<*>{
        return EnumGene<String>("rand EnumGene", listOf("A","B","C"))
    }


    fun sampleDisruptiveGene(rand: Randomness) : DisruptiveGene<*>{
        val selection = geneClasses.filter { it != DisruptiveGene::class }
        val chosen = sample(rand.choose(selection), rand)

        return DisruptiveGene("rand DisruptiveGene", chosen, 0.5)
    }

    fun sampleCycleObjectGene(rand: Randomness) : CycleObjectGene{
        return CycleObjectGene("rand CycleObjectGene ${rand.nextInt()}")
    }


    fun sampleBooleanGene(rand: Randomness) : BooleanGene{
        return BooleanGene(name="rand boolean ${rand.nextInt()}")
    }

    fun sampleDoubleGene(rand: Randomness) : DoubleGene{
        val min = rand.nextDouble()

        return DoubleGene(
                name = "rand DoubleGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + rand.nextDouble())),
                minInclusive = rand.nextBoolean(),
                maxInclusive = rand.nextBoolean(),
                precision = rand.choose(listOf(null, rand.nextInt())),
                scale = rand.choose(listOf(null, rand.nextInt()))
        )
    }


    fun sampleIntegerGene(rand: Randomness) : IntegerGene{
        val min = rand.nextInt() / 2

        return IntegerGene(
                name = "rand IntegerGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + rand.nextInt()/2)),
                minInclusive = rand.nextBoolean(),
                maxInclusive = rand.nextBoolean(),
                precision = rand.choose(listOf(null, rand.nextInt())),
        )
    }

    fun sampleLongGene(rand: Randomness) : LongGene{
        val min = rand.nextLong() / 2

        return LongGene(
                name = "rand LongGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + rand.nextLong()/2)),
                minInclusive = rand.nextBoolean(),
                maxInclusive = rand.nextBoolean(),
                precision = rand.choose(listOf(null, rand.nextInt())),
        )
    }

    fun sampleFloatGene(rand: Randomness) : FloatGene{
        val min = rand.nextFloat()

        return FloatGene(
                name = "rand FloatGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + rand.nextFloat())),
                minInclusive = rand.nextBoolean(),
                maxInclusive = rand.nextBoolean(),
                precision = rand.choose(listOf(null, rand.nextInt())),
                scale = rand.choose(listOf(null, rand.nextInt()))
        )
    }

    fun sampleBigDecimalGene(rand: Randomness) : BigDecimalGene{

        val min = rand.nextLong()

        return BigDecimalGene(
              name = "rand BigDecimalGene ${rand.nextInt()}",
              min = rand.choose(listOf(null, BigDecimal(min))),
              max = rand.choose(listOf(null, BigDecimal(min + rand.nextDouble()))),
              minInclusive = rand.nextBoolean(),
              maxInclusive = rand.nextBoolean(),
              floatingPointMode = rand.nextBoolean(),
              precision = rand.choose(listOf(null, rand.nextInt())),
              scale = rand.choose(listOf(null, rand.nextInt()))
        )
    }

    fun sampleArrayGene(rand: Randomness): ArrayGene<*> {

        val selection = geneClasses // TODO might filter out some genes here
        val chosen = sample(rand.choose(selection), rand)

        return ArrayGene("rand array ${rand.nextInt()}", chosen)
    }

    fun sampleBase64StringGene(rand: Randomness): Base64StringGene{
        return Base64StringGene("rand Base64StringGene ${rand.nextInt()}")
    }

    fun sampleStringGene(rand: Randomness): StringGene {

        val min = rand.nextInt(0, 3)
        val max = min + rand.nextInt(20)

        return StringGene("rand string ${rand.nextInt()}", minLength = min, maxLength = max)
    }

}