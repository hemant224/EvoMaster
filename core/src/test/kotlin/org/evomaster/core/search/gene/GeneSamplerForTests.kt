package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.search.gene.sql.*
import org.evomaster.core.search.gene.sql.geometric.*
import org.evomaster.core.search.gene.sql.network.SqlCidrGene
import org.evomaster.core.search.gene.sql.network.SqlInetGene
import org.evomaster.core.search.gene.sql.network.SqlMacAddrGene
import org.evomaster.core.search.gene.sql.textsearch.SqlTextSearchQueryGene
import org.evomaster.core.search.gene.sql.textsearch.SqlTextSearchVectorGene
import org.evomaster.core.search.gene.sql.time.SqlTimeIntervalGene
import org.evomaster.core.search.service.Randomness
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
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

                when genes need input genes, we sample those at random as well
             */
            ArrayGene::class -> sampleArrayGene(rand) as T
            Base64StringGene::class -> sampleBase64StringGene(rand) as T
            BigDecimalGene::class -> sampleBigDecimalGene(rand) as T
            BigIntegerGene::class -> sampleBigIntegerGene(rand) as T
            BooleanGene::class -> sampleBooleanGene(rand) as T
            CycleObjectGene::class -> sampleCycleObjectGene(rand) as T
            DisruptiveGene::class -> sampleDisruptiveGene(rand) as T
            DoubleGene::class -> sampleDoubleGene(rand) as T
            EnumGene::class -> sampleEnumGene(rand) as T
            FloatGene::class -> sampleFloatGene(rand) as T
            ImmutableDataHolderGene::class -> sampleImmutableDataHolderGene(rand) as T
            IntegerGene::class -> sampleIntegerGene(rand) as T
            LimitObjectGene::class -> sampleLimitObjectGene(rand) as T
            LongGene::class -> sampleLongGene(rand) as T
            MapGene::class -> sampleMapGene(rand) as T
            NumericStringGene::class -> sampleNumericStringGene(rand) as T
            ObjectGene::class -> sampleObjectGene(rand) as T
            OptionalGene::class -> sampleOptionalGene(rand) as T
            PairGene::class -> samplePairGene(rand) as T
            SeededGene::class -> sampleSeededGene(rand) as T
            StringGene::class -> sampleStringGene(rand) as T
            TupleGene::class -> sampleTupleGene(rand) as T
            DateGene::class -> sampleDateGene(rand) as T
            DateTimeGene::class -> sampleDateGene(rand) as T
            TimeGene::class -> sampleTimeGene(rand) as T
            AnyCharacterRxGene::class -> sampleAnyCharacterRxGene(rand) as T
            CharacterClassEscapeRxGene::class -> sampleCharacterClassEscapeRxGene(rand) as T
            CharacterRangeRxGene::class -> sampleCharacterRangeRxGene(rand) as T
            DisjunctionRxGene::class -> sampleDisjunctionRxGene(rand) as T
            DisjunctionListRxGene::class -> sampleDisjunctionListRxGene(rand) as T
            PatternCharacterBlockGene::class -> samplePatternCharacterBlock(rand) as T
            QuantifierRxGene::class -> sampleQuantifierRxGene(rand) as T
            RegexGene::class -> sampleRegexGene(rand) as T

            //SQL genes
            SqlJSONPathGene::class -> sampleSqlJSONPathGene(rand) as T
            SqlTextSearchVectorGene::class -> sampleSqlTextSearchVectorGene(rand) as T
            SqlBoxGene::class -> sampleSqlBoxGene(rand) as T
            SqlPointGene::class -> sampleSqlPointGene(rand) as T
            SqlForeignKeyGene::class -> sampleSqlForeignKeyGene(rand) as T
            SqlLogSeqNumberGene::class -> sampleSqlLogSeqNumberGene(rand) as T
            SqlRangeGene::class -> sampleSqlRangeGene(rand) as T
            SqlJSONGene::class -> sampleSqlJSONGene(rand) as T
            SqlTextSearchQueryGene::class -> sampleSqlTextSearchQueryGene(rand) as T
            SqlPrimaryKeyGene::class -> sampleSqlPrimaryKeyGene(rand) as T
            SqlNullableGene::class -> sampleSqlNullableGene(rand) as T
            SqlMultidimensionalArrayGene::class -> sampleSqlMultidimensionalArrayGene(rand) as T
            SqlMacAddrGene::class -> sampleSqlMacAddrGene(rand) as T
            SqlInetGene::class -> sampleSqlInetGene(rand) as T
            SqlCidrGene::class -> sampleSqlCidrGene(rand) as T
            SqlAutoIncrementGene::class -> sampleSqlAutoIncrementGene(rand) as T
            SqlPathGene::class -> sampleSqlPathGene(rand) as T
            SqlLineGene::class -> sampleSqlLineGene(rand) as T
            SqlPolygonGene::class -> sampleSqlPolygonGene(rand) as T
            SqlCircleGene::class -> sampleSqlCircleGene(rand) as T
            SqlLineSegmentGene::class -> sampleSqlLineSegmentGene(rand) as T
            SqlTimeIntervalGene::class -> sampleSqlTimeIntervalGene(rand) as T
            SqlCompositeGene::class -> sampleSqlCompositeGene(rand) as T
            SqlBitStringGene::class -> sampleSqlBitStringGene(rand) as T
            SqlXMLGene::class -> sampleSqlXMLGene(rand) as T
            SqlMultiRangeGene::class -> sampleSqlMultiRangeGene(rand) as T
            SqlBinaryStringGene::class -> sampleSqlBinaryStringGene(rand) as T
            SqlUUIDGene::class -> sampleSqlUUIDGene(rand) as T

            else -> throw IllegalStateException("No sampler for $klass")
        }
    }

    private fun sampleSqlUUIDGene(rand: Randomness): SqlUUIDGene {
        return SqlUUIDGene("rand SqlUUIDGene ${rand.nextInt()}")
    }

    private fun sampleSqlBinaryStringGene(rand: Randomness): SqlBinaryStringGene {
        val maxSize = rand.nextInt(1, ArrayGene.MAX_SIZE)
        val minSize = rand.nextInt(0, maxSize)
        return SqlBinaryStringGene("rand SqlBinaryStringGene",
                minSize = minSize,
                maxSize = maxSize)
    }

    private fun sampleSqlMultiRangeGene(rand: Randomness): SqlMultiRangeGene<*> {
        return SqlMultiRangeGene("rand SqlMultiRangeGene", template = sampleSqlRangeGene(rand))
    }

    private fun sampleSqlXMLGene(rand: Randomness): SqlXMLGene {
        return SqlXMLGene("rand SqlXMLGene ${rand.nextInt()}")
    }

    private fun sampleSqlBitStringGene(rand: Randomness): SqlBitStringGene {
        val maxSize = rand.nextInt(1, ArrayGene.MAX_SIZE)
        val minSize = rand.nextInt(0, maxSize)
        return SqlBitStringGene("rand SqlBitStringGene",
                minSize = minSize,
                maxSize = maxSize)
    }

    private fun sampleSqlCompositeGene(rand: Randomness): SqlCompositeGene {
        val selection = geneClasses.filter { !it.isAbstract }

        val numberOfFields = rand.nextInt(1,MAX_NUMBER_OF_FIELDS)
        return SqlCompositeGene(
                name = "rand SqlCompositeGene",
                fields = List(numberOfFields) { sample(rand.choose(selection), rand) }
        )
    }

    private fun sampleSqlTimeIntervalGene(rand: Randomness): SqlTimeIntervalGene {
        val timeGeneFormats = listOf(TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT,
                TimeGene.TimeGeneFormat.TIME_WITH_MILLISECONDS)
        val timeGeneFormat = rand.choose(timeGeneFormats)
        return SqlTimeIntervalGene("rand SqlTimeIntervalGene",
                time = TimeGene("hoursMinutesAndSeconds", timeGeneFormat = timeGeneFormat))
    }

    private fun sampleSqlLineSegmentGene(rand: Randomness): SqlLineSegmentGene {
        return SqlLineSegmentGene("rand SqlLineSegmentGene ${rand.nextInt()}")
    }

    private fun sampleSqlCircleGene(rand: Randomness): SqlCircleGene {
        return SqlCircleGene("rand SqlCircleGene ${rand.nextInt()}")
    }

    private fun sampleSqlPolygonGene(rand: Randomness): SqlPolygonGene {
        return SqlPolygonGene("rand SqlPolygonGene ${rand.nextInt()}")
    }

    private fun sampleSqlLineGene(rand: Randomness): SqlLineGene {
        return SqlLineGene("rand SqlLineGene ${rand.nextInt()}")
    }

    private fun sampleSqlPathGene(rand: Randomness): SqlPathGene {
        return SqlPathGene("rand SqlPathGene ${rand.nextInt()}")
    }

    private fun sampleSqlAutoIncrementGene(rand: Randomness): SqlAutoIncrementGene {
        return SqlAutoIncrementGene("rand SqlAutoIncrementGene ${rand.nextInt()}")
    }

    private fun sampleSqlCidrGene(rand: Randomness): SqlCidrGene {
        return SqlCidrGene("rand SqlCidrGene ${rand.nextInt()}")
    }

    private fun sampleSqlInetGene(rand: Randomness): SqlInetGene {
        return SqlInetGene("rand SqlInetGene ${rand.nextInt()}")
    }

    private fun sampleSqlMacAddrGene(rand: Randomness): SqlMacAddrGene {
        return SqlMacAddrGene("rand SqlMacAddrGene ${rand.nextInt()}",
                numberOfOctets = rand.nextInt(1, MAX_NUMBER_OF_OCTETS))
    }

    const val MAX_NUMBER_OF_DIMENSIONS = 5
    const val MAX_NUMBER_OF_OCTETS = 10
    const val MAX_NUMBER_OF_FIELDS = 3

    private fun selectionForArrayTemplate() : List<KClass<out Gene>>{
        return geneClasses
                .filter { !it.isAbstract }
                .filter { it.java != CycleObjectGene::class.java && it.java !== LimitObjectGene::class.java}
                .filter { it.java != ArrayGene::class.java && it.java != SqlMultidimensionalArrayGene::class.java}
        // TODO might filter out some more genes here
    }

    private fun sampleSqlMultidimensionalArrayGene(rand: Randomness): SqlMultidimensionalArrayGene<*> {

        val selection = selectionForArrayTemplate()

        return SqlMultidimensionalArrayGene("rand SqlMultidimensionalArrayGene",
                template = sample(rand.choose(selection), rand),
                numberOfDimensions = rand.nextInt(1, MAX_NUMBER_OF_DIMENSIONS))
    }

    private fun sampleSqlNullableGene(rand: Randomness): SqlNullableGene {
        val selection = geneClasses.filter { !it.isAbstract }
                .filter { it.java != SqlForeignKeyGene::class.java }
        return SqlNullableGene("rand SqlNullableGene",
                gene = sample(rand.choose(selection), rand))
    }

    private fun sampleSqlPrimaryKeyGene(rand: Randomness): SqlPrimaryKeyGene {
        val selection = geneClasses.filter { !it.isAbstract && it.isSubclassOf(ComparableGene::class) }

        return SqlPrimaryKeyGene("rand SqlPrimaryKeyGene",
                tableName = "rand tableName",
                gene = sample(rand.choose(selection), rand),
                uniqueId = rand.nextLong(0, Long.MAX_VALUE))
    }

    private fun sampleSqlTextSearchQueryGene(rand: Randomness): SqlTextSearchQueryGene {
        return SqlTextSearchQueryGene("rand SqlTextSearchQueryGene ${rand.nextInt()}")
    }

    private fun sampleSqlJSONGene(rand: Randomness): SqlJSONGene {
        return SqlJSONGene("rand SqlJSONGene ${rand.nextInt()}")
    }

    private fun sampleSqlRangeGene(rand: Randomness): SqlRangeGene<*> {
        val selection = geneClasses.filter { !it.isAbstract && it.isSubclassOf(ComparableGene::class) }
        val selectedClass = rand.choose(selection)
        val templateSample = sample(selectedClass, rand)
        if (templateSample !is ComparableGene) {
            throw IllegalStateException("${templateSample::class.java} does not implement ComparableGene")
        }
        return SqlRangeGene(
                "rand SqlRangeGene",
                template = templateSample)

    }

    private fun sampleSqlLogSeqNumberGene(rand: Randomness): SqlLogSeqNumberGene {
        return SqlLogSeqNumberGene("rand SqlLogSeqNumberGene ${rand.nextInt()}")
    }

    private fun sampleSqlForeignKeyGene(rand: Randomness): SqlForeignKeyGene {
        return SqlForeignKeyGene(sourceColumn = "rand source column",
                uniqueId = rand.nextLong(min = 0L, max = Long.MAX_VALUE),
                targetTable = "rand target table",
                nullable = rand.nextBoolean(),
                uniqueIdOfPrimaryKey = rand.nextLong())
    }

    private fun sampleSqlPointGene(rand: Randomness): SqlPointGene {
        return SqlPointGene("rand SqlPointGene ${rand.nextInt()}")
    }

    private fun sampleSqlBoxGene(rand: Randomness): SqlBoxGene {
        return SqlBoxGene("rand SqlBoxGene ${rand.nextInt()}")
    }

    private fun sampleSqlTextSearchVectorGene(rand: Randomness): SqlTextSearchVectorGene {
        return SqlTextSearchVectorGene("rand SqlTextSearchVectorGene ${rand.nextInt()}")

    }

    private fun sampleSqlJSONPathGene(rand: Randomness): SqlJSONPathGene {
        return SqlJSONPathGene("rand JSONPathGene ${rand.nextInt()}")
    }

    fun sampleRegexGene(rand: Randomness): RegexGene {
        return RegexGene(name = "rand RegexGene", disjunctions = sampleDisjunctionListRxGene(rand))
    }

    fun sampleQuantifierRxGene(rand: Randomness): QuantifierRxGene {

        val selection = geneClasses
                .filter { !it.isAbstract }
                .filter { it.isSubclassOf(RxAtom::class) }
        val min = rand.nextInt(2)

        return QuantifierRxGene(
                name = "rand QuantifierRxGene",
                template = sample(rand.choose(selection), rand),
                min = min,
                max = min + rand.nextInt(1, 2)
        )
    }

    fun samplePatternCharacterBlock(rand: Randomness): PatternCharacterBlockGene {
        return PatternCharacterBlockGene(name = "rand PatternCharacterBlock", stringBlock = rand.nextWordString())
    }

    fun sampleDisjunctionListRxGene(rand: Randomness): DisjunctionListRxGene {

        return DisjunctionListRxGene(listOf(
                sampleDisjunctionRxGene(rand),
                sampleDisjunctionRxGene(rand)
        ))
    }

    fun sampleDisjunctionRxGene(rand: Randomness): DisjunctionRxGene {

        val selection = geneClasses
                .filter { !it.isAbstract }
                .filter { it.isSubclassOf(RxTerm::class) }
                //let's avoid huge trees...
                .filter { (it.java != DisjunctionListRxGene::class.java && it.java != DisjunctionRxGene::class.java)
                        || rand.nextBoolean() }

        val numberOfTerms = rand.nextInt(1, 3)
        return DisjunctionRxGene(
                name = "rand DisjunctionRxGene",
                terms = List(numberOfTerms) { sample(rand.choose(selection), rand) },
                matchStart = rand.nextBoolean(),
                matchEnd = rand.nextBoolean()
        )
    }

    fun sampleCharacterRangeRxGene(rand: Randomness): CharacterRangeRxGene {
        return CharacterRangeRxGene(
                negated = false, // TODO update once fixed
                ranges = listOf(Pair('a', 'z'))
        )
    }

    fun sampleCharacterClassEscapeRxGene(rand: Randomness): CharacterClassEscapeRxGene {
        return CharacterClassEscapeRxGene(type = rand.choose(listOf("w", "W", "d", "D", "s", "S")))
    }

    fun sampleAnyCharacterRxGene(rand: Randomness): AnyCharacterRxGene {
        return AnyCharacterRxGene()
    }

    fun sampleTimeGene(rand: Randomness): TimeGene {
        return TimeGene(name = "rand TimeGene")
    }

    fun sampleDateTimeGene(rand: Randomness): DateTimeGene {
        return DateTimeGene("rand DateTimeGene")
    }

    fun sampleDateGene(rand: Randomness): DateGene {
        return DateGene(name = "rand DateGene", onlyValidDates = rand.nextBoolean())
    }

    fun sampleSeededGene(rand: Randomness): SeededGene<*> {

        //TODO update after refactoring SeededGene with ChoiceGene (to implement)

        return SeededGene(
                name = "rand SeededGene",
                gene = sampleStringGene(rand),
                seeded = sampleEnumGene(rand) as EnumGene<StringGene>,
                employSeeded = rand.nextBoolean()
        )
    }

    fun sampleTupleGene(rand: Randomness): TupleGene {

        val selection = geneClasses.filter { !it.isAbstract }

        return TupleGene(
                name = "rand TupleGene ${rand.nextInt()}",
                elements = listOf(
                        sample(rand.choose(selection), rand),
                        sample(rand.choose(selection), rand),
                        sample(rand.choose(selection), rand)
                ),
                lastElementTreatedSpecially = rand.nextBoolean()

        )
    }

    fun samplePairGene(rand: Randomness): PairGene<*, *> {

        val selection = geneClasses.filter { !it.isAbstract }

        return PairGene(
                name = "rand PairGene",
                first = sample(rand.choose(selection), rand),
                second = sample(rand.choose(selection), rand),
                isFirstMutable = rand.nextBoolean()
        )
    }

    fun sampleOptionalGene(rand: Randomness): OptionalGene {

        val selection = geneClasses.filter { !it.isAbstract }

        return OptionalGene(
                name = "rand OptionalGene",
                gene = sample(rand.choose(selection), rand)
        )
    }

    fun sampleObjectGene(rand: Randomness): ObjectGene {

        val selection = geneClasses.filter { !it.isAbstract }

        return ObjectGene(
                name = "rand ObjectGene ${rand.nextInt()}",
                fields = listOf(
                        sample(rand.choose(selection), rand),
                        sample(rand.choose(selection), rand),
                        sample(rand.choose(selection), rand)
                )
        )
    }

    fun sampleNumericStringGene(rand: Randomness): NumericStringGene {
        return NumericStringGene(
                name = "rand NumericStringGene",
                minLength = rand.nextInt(2),
                number = sample(BigDecimalGene::class, rand)
        )
    }

    fun sampleMapGene(rand: Randomness): MapGene<*, *> {

        val min = rand.nextInt(0, 2)

        return MapGene(
                name = "rand MapGene",
                minSize = rand.choose(listOf(null, min)),
                maxSize = rand.choose(listOf(null, min + rand.nextInt(1, 3))),
                template = sample(PairGene::class, rand)
        )
    }

    fun sampleLimitObjectGene(rand: Randomness): LimitObjectGene {
        return LimitObjectGene(name = "rand LimitObjectGene")
    }

    fun sampleImmutableDataHolderGene(rand: Randomness): ImmutableDataHolderGene {
        return ImmutableDataHolderGene(
                name = "rand ImmutableDataHolderGene",
                value = rand.nextWordString(),
                inQuotes = rand.nextBoolean()
        )
    }

    fun sampleEnumGene(rand: Randomness): EnumGene<*> {
        return EnumGene<String>("rand EnumGene ${rand.nextInt()}", listOf("A", "B", "C"))
    }


    fun sampleDisruptiveGene(rand: Randomness): DisruptiveGene<*> {
        val selection = geneClasses
                .filter { !it.isAbstract }
                .filter { it != DisruptiveGene::class }
        val chosen = sample(rand.choose(selection), rand)

        return DisruptiveGene("rand DisruptiveGene", chosen, 0.5)
    }

    fun sampleCycleObjectGene(rand: Randomness): CycleObjectGene {
        return CycleObjectGene("rand CycleObjectGene ${rand.nextInt()}")
    }


    fun sampleBooleanGene(rand: Randomness): BooleanGene {
        return BooleanGene(name = "rand boolean ${rand.nextInt()}")
    }

    fun sampleDoubleGene(rand: Randomness): DoubleGene {
        val scale : Int? = rand.choose(listOf(null, rand.nextInt(0, 2)))

        // if scale is 0, to distinguish min and max
        val min = rand.nextDouble().run {
            // format min based on scale with 50%
            if (rand.nextBoolean())
                NumberMutatorUtils.getFormattedValue(this, scale, RoundingMode.UP)
            else
                this
        }

        val least = getMinPrecision(min)
        val precision = max(min(least + rand.nextInt(0, 10), 308), least) + (scale?:0)

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()

        val actualScale = getScale(min)
        val delta : Double = (if(!minInclusive || !maxInclusive) 2.0 else 0.0).run { if (scale!= null && actualScale > scale) this+2.0 else this }

        return DoubleGene(
                name = "rand DoubleGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + delta + rand.nextDouble())),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
                scale = scale
        )
    }


    fun sampleIntegerGene(rand: Randomness): IntegerGene {
        val min = rand.nextInt() / 2

        val least = getMinPrecision(min)
        val precision = max(min(least + rand.nextInt(0, 2), 8), least)

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()
        val delta = if (!minInclusive && !maxInclusive) 3 // consider randomize with new value, we might employ delta 3 instead of 2
                    else if(!minInclusive || !maxInclusive) 2 else 0


        return IntegerGene(
                name = "rand IntegerGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + delta + rand.nextInt(0, 100))),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
        )
    }

    fun sampleLongGene(rand: Randomness): LongGene {
        val min = rand.nextLong() / 2

        val least = getMinPrecision(min)
        val precision = max(min(least + rand.nextInt(0, 2), 10), least)


        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()
        val minDelta = if (!minInclusive && !maxInclusive) 3 else 2

        return LongGene(
                name = "rand LongGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + rand.nextInt(minDelta, 100))),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
        )
    }

    fun sampleFloatGene(rand: Randomness): FloatGene {
        val scale : Int? = rand.choose(listOf(null, rand.nextInt(0, 2)))

        val min = rand.nextFloat().run {
            // format min based on scale with 50%
            if (rand.nextBoolean())
                NumberMutatorUtils.getFormattedValue(this, scale, RoundingMode.UP)
            else
                this
        }

        val least = getMinPrecision(min)

        val precision = max(min(least + rand.nextInt(0, 2), 12), least) + (scale?:0)

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()

        val actualScale = getScale(min)
        val delta : Float = (if(!minInclusive || !maxInclusive) 2.0f else 0.0f).run { if (scale!= null && actualScale > scale) this+2.0f else this }

        return FloatGene(
                name = "rand FloatGene ${rand.nextInt()}",
                min = rand.choose(listOf(null, min)),
                max = rand.choose(listOf(null, min + delta + abs(rand.nextFloat()))),
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
                scale = scale
        )
    }

    fun sampleBigDecimalGene(rand: Randomness): BigDecimalGene {

        val scale : Int? = rand.choose(listOf(null, rand.nextInt(0, 2)))

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()

        val minBigDecimal: BigDecimal?
        val maxBigDecimal: BigDecimal?
        if (rand.nextBoolean()) {
            minBigDecimal = null
            maxBigDecimal = null
        } else {
            val min = rand.nextLong() / 2
            minBigDecimal = BigDecimal.valueOf(min)

            val minDelta : Long = if (!minInclusive && !maxInclusive) 3 else 2
            val addition =  if (minBigDecimal.toDouble() >= 0) BigDecimal.valueOf(Long.MAX_VALUE).subtract(minBigDecimal).toLong() else Long.MAX_VALUE
            maxBigDecimal = minBigDecimal + BigDecimal.valueOf(max(minDelta, rand.nextLong(0,addition) / 2))
        }

        val least = if (minBigDecimal != null) getMinPrecision(minBigDecimal) else rand.nextInt(1, 5)

        val precision = max(min(least + rand.nextInt(0, 2), 12), least) + (scale?:0)

        return BigDecimalGene(
                name = "rand BigDecimalGene ${rand.nextInt()}",
                min = minBigDecimal,
                max = maxBigDecimal,
                minInclusive = if (minBigDecimal==null) true else minInclusive,
                maxInclusive = if (maxBigDecimal==null) true else maxInclusive,
                floatingPointMode = rand.nextBoolean(),
                precision = rand.choose(listOf(null, precision)),
                scale = scale
        )
    }

    fun sampleBigIntegerGene(rand: Randomness): BigIntegerGene {
        val minBigInteger: BigInteger?
        val maxBigInteger: BigInteger?

        val minInclusive = rand.nextBoolean()
        val maxInclusive = rand.nextBoolean()
        val minDelta = if (!minInclusive && !maxInclusive) 3L else 2L

        if (rand.nextBoolean()) {
            minBigInteger = null
            maxBigInteger = null
        } else {
            minBigInteger = BigInteger.valueOf(rand.nextLong() / 2)
            maxBigInteger = minBigInteger.plus(BigInteger.valueOf(max(minDelta, rand.nextLong(0, Long.MAX_VALUE) / 2 )))
        }

        val least = if (minBigInteger != null) getMinPrecision(minBigInteger) else rand.nextInt(1, 5)
        val precision = max(min(least + rand.nextInt(0, 2), 12), least)

        return BigIntegerGene(
                name = "rand BigIntegerGene ${rand.nextInt()}",
                min = minBigInteger,
                max = maxBigInteger,
                minInclusive = minInclusive,
                maxInclusive = maxInclusive,
                precision = rand.choose(listOf(null, precision)),
        )
    }

    fun sampleArrayGene(rand: Randomness): ArrayGene<*> {

        val selection = selectionForArrayTemplate()
        val chosen = sample(rand.choose(selection), rand)

        return ArrayGene("rand array ${rand.nextInt()}", chosen)
    }

    fun sampleBase64StringGene(rand: Randomness): Base64StringGene {
        return Base64StringGene("rand Base64StringGene ${rand.nextInt()}")
    }

    fun sampleStringGene(rand: Randomness): StringGene {

        val min = rand.nextInt(0, 3)
        val max = min + rand.nextInt(20)

        return StringGene("rand string ${rand.nextInt()}", minLength = min, maxLength = max)
    }

    private fun getMinPrecision(value : Number) : Int{
        return value.toString().split(".")[0].replace("-","").length
    }

    private fun getScale(value: Number): Int{
        return value.toString().run {
            if (!contains(".")) 0
            else split(".")[1].length
        }
    }

}