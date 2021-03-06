/**
 * Copyright (C) 2013 Klout Inc. <http://www.klout.com>
 */

package com.klout.scoozie
package conversion

import dsl._
import jobs._
import samples._
import org.specs2.mutable._

class FlattenSpec extends Specification {

    "Flatten" should {

        "give empty result for empty Workflow" in {
            Flatten(EmptyWorkflow).values.toSet must beEmpty
        }
        "give single node for single node workflow" in {
            val first = GraphNode("start", WorkflowJob(NoOpJob("start")))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(end)

            Flatten(SingleWorkflow).values.toSet must_== Set(first)
        }
        "work for simple flow" in {
            val a = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val b = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val c = GraphNode("third", WorkflowJob(NoOpJob("third")))
            val d = GraphNode("fourth", WorkflowJob(NoOpJob("fourth")))
            val end = GraphNode("end", WorkflowEnd)

            a.after = RefSet(b)
            b.before = RefSet(a)
            b.after = RefSet(c)
            c.before = RefSet(b)
            c.after = RefSet(d)
            d.before = RefSet(c)
            d.after = RefSet(end)

            Flatten(SimpleWorkflow).values.toSet must_== Set(a, b, c, d)
        }
        "work for simple fork / join" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val fork = GraphNode("fork-secondA-secondB", WorkflowFork)
            val a = GraphNode("secondA", WorkflowJob(NoOpJob("secondA")))
            val b = GraphNode("secondB", WorkflowJob(NoOpJob("secondB")))
            val join = GraphNode("join-secondA-secondB", WorkflowJoin)

            first.after = RefSet(fork)
            fork.before = RefSet(first)
            fork.after = RefSet(a, b)
            a.before = RefSet(fork)
            a.after = RefSet(join)
            b.before = RefSet(fork)
            b.after = RefSet(join)
            join.before = RefSet(a, b)

            Flatten(SimpleForkJoin).values.toSet must_== Set(first, fork, a, b, join)
        }
        "work for fork / join only" in {
            val fork = GraphNode("fork-startA-startB", WorkflowFork)
            val a = GraphNode("startA", WorkflowJob(NoOpJob("startA")))
            val b = GraphNode("startB", WorkflowJob(NoOpJob("startB")))
            val join = GraphNode("join-startA-startB", WorkflowJoin)

            fork.after = RefSet(a, b)
            a.before = RefSet(fork)
            a.after = RefSet(join)
            b.before = RefSet(fork)
            b.after = RefSet(join)
            join.before = RefSet(a, b)

            Flatten(ForkJoinOnly).values.toSet must_== Set(fork, a, b, join)
        }
        "work for simple sub workflow" in {
            val first = GraphNode("begin", WorkflowJob(NoOpJob("begin")))
            val a = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val b = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val c = GraphNode("third", WorkflowJob(NoOpJob("third")))
            val d = GraphNode("fourth", WorkflowJob(NoOpJob("fourth")))
            val end = GraphNode("end", WorkflowEnd)

            a.after = RefSet(b)
            b.before = RefSet(a)
            b.after = RefSet(c)
            c.before = RefSet(b)
            c.after = RefSet(d)
            d.before = RefSet(c)
            val third = GraphNode("final", WorkflowJob(NoOpJob("final")))

            first.after = RefSet(a)
            a.before = RefSet(first)
            d.after = RefSet(third)
            third.before = RefSet(d)
            third.after = RefSet(end)

            Flatten(SimpleSubWorkflow).values.toSet must_== Set(first, a, b, c, d, third)
        }

        "work with two separate fork / joins" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val fork = GraphNode("fork-secondA-secondB", WorkflowFork)
            val a = GraphNode("secondA", WorkflowJob(NoOpJob("secondA")))
            val b = GraphNode("secondB", WorkflowJob(NoOpJob("secondB")))
            val join = GraphNode("join-secondA-secondB", WorkflowJoin)
            val third = GraphNode("third", WorkflowJob(NoOpJob("third")))
            val fork2 = GraphNode("fork-fourthA-fourthB", WorkflowFork)
            val fa = GraphNode("fourthA", WorkflowJob(NoOpJob("fourthA")))
            val fb = GraphNode("fourthB", WorkflowJob(NoOpJob("fourthB")))
            val join2 = GraphNode("join-fourthA-fourthB", WorkflowJoin)

            first.after = RefSet(fork)
            fork.before = RefSet(first)
            fork.after = RefSet(a, b)
            a.before = RefSet(fork)
            a.after = RefSet(join)
            b.before = RefSet(fork)
            b.after = RefSet(join)
            join.before = RefSet(b, a)
            join.after = RefSet(third)
            third.before = RefSet(join)
            third.after = RefSet(fork2)
            fork2.before = RefSet(third)
            fork2.after = RefSet(fa, fb)
            fa.before = RefSet(fork2)
            fa.after = RefSet(join2)
            fb.before = RefSet(fork2)
            fb.after = RefSet(join2)
            join2.before = RefSet(fb, fa)

            Flatten(TwoSimpleForkJoins).values.toSet must_== Set(first, fork, a, b, join, third, fork2, fa, fb, join2)
        }

        "work with subworkflow and fork / joins" in {
            val first = GraphNode("start", WorkflowJob(NoOpJob("start")))
            val a = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val b = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val c = GraphNode("third", WorkflowJob(NoOpJob("third")))
            val d = GraphNode("fourth", WorkflowJob(NoOpJob("fourth")))
            val fork = GraphNode("fork-thirdA-thirdB", WorkflowFork)
            val thirdA = GraphNode("thirdA", WorkflowJob(NoOpJob("thirdA")))
            val thirdB = GraphNode("thirdB", WorkflowJob(NoOpJob("thirdB")))
            val join = GraphNode("join-thirdA-thirdB", WorkflowJoin)

            first.after = RefSet(a)
            a.before = RefSet(first)
            a.after = RefSet(b)
            b.before = RefSet(a)
            b.after = RefSet(c)
            c.before = RefSet(b)
            c.after = RefSet(d)
            d.before = RefSet(c)
            d.after = RefSet(fork)
            fork.before = RefSet(d)
            fork.after = RefSet(thirdA, thirdB)
            thirdA.before = RefSet(fork)
            thirdA.after = RefSet(join)
            thirdB.before = RefSet(fork)
            thirdB.after = RefSet(join)
            join.before = RefSet(thirdA, thirdB)

            Flatten(SubworkflowWithForkJoins).values.toSet must_== Set(first, a, b, c, d, fork, thirdA, thirdB, join)
        }

        "work with nested fork / joins" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val fork1 = GraphNode("fork-secondA-secondB", WorkflowFork)
            val secondA = GraphNode("secondA", WorkflowJob(NoOpJob("secondA")))
            val secondB = GraphNode("secondB", WorkflowJob(NoOpJob("secondB")))
            val fork2 = GraphNode("fork-thirdA-thirdB", WorkflowFork)
            val thirdA = GraphNode("thirdA", WorkflowJob(NoOpJob("thirdA")))
            val thirdB = GraphNode("thirdB", WorkflowJob(NoOpJob("thirdB")))
            val thirdC = GraphNode("thirdC", WorkflowJob(NoOpJob("thirdC")))
            val join = GraphNode("join-thirdA-thirdB-thirdC", WorkflowJoin)
            val fourth = GraphNode("fourth", WorkflowJob(NoOpJob("fourth")))

            first.after = RefSet(fork1)
            fork1.before = RefSet(first)
            fork1.after = RefSet(secondA, secondB)
            secondA.before = RefSet(fork1)
            secondA.after = RefSet(fork2)
            secondB.before = RefSet(fork1)
            secondB.after = RefSet(thirdC)
            fork2.before = RefSet(secondA)
            fork2.after = RefSet(thirdA, thirdB)
            thirdA.before = RefSet(fork2)
            thirdA.after = RefSet(join)
            thirdB.before = RefSet(fork2)
            thirdB.after = RefSet(join)
            thirdC.before = RefSet(secondB)
            thirdC.after = RefSet(join)
            join.before = RefSet(thirdA, thirdB, thirdC)
            join.after = RefSet(fourth)
            fourth.before = RefSet(join)

            Flatten(NestedForkJoin).values.toSet must_== Set(first, fork1, secondA, secondB, fork2, thirdA, thirdB, thirdC, join, fourth)
        }

        "work with simple decision" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-option", WorkflowDecision(List("route1" -> Predicates.AlwaysTrue)))
            val default = GraphNode("default", WorkflowJob(NoOpJob("default")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("route1"))
            val second = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(default, option)
            default.decisionAfter = RefSet(second)
            option.decisionAfter = RefSet(second)
            second.decisionBefore = RefSet(default, option)
            second.after = RefSet(end)

            Flatten(SimpleDecision).values.toSet must_== Set(first, decision, second, default, option)
        }

        "work with more complex decision" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-option", WorkflowDecision(List("route1" -> Predicates.AlwaysTrue)))
            val default = GraphNode("default", WorkflowJob(NoOpJob("default")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val default2 = GraphNode("default2", WorkflowJob(NoOpJob("default2")), RefSet(), RefSet(), RefSet(), RefSet())
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("route1"))
            val second = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(default, option)
            default.after = RefSet(default2)
            default2.before = RefSet(default)
            default2.decisionAfter = RefSet(second)
            option.decisionAfter = RefSet(second)
            second.decisionBefore = RefSet(default2, option)
            second.after = RefSet(end)

            Flatten(MoreComplexDecision).values.toSet must_== Set(first, decision, second, default, default2, option)
        }

        "work with multiple decisions" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-job3", WorkflowDecision(List("route1" -> Predicates.AlwaysTrue)))
            val decision2 = GraphNode("decision-job2", WorkflowDecision(List("route1" -> Predicates.AlwaysTrue)), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val job = GraphNode("job", WorkflowJob(NoOpJob("job")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val job2 = GraphNode("job2", WorkflowJob(NoOpJob("job2")), RefSet(), RefSet(), RefSet(), RefSet(), Some("route1"))
            val job3 = GraphNode("job3", WorkflowJob(NoOpJob("job3")), RefSet(), RefSet(), RefSet(), RefSet(), Some("route1"))
            val fourth = GraphNode("fourth", WorkflowJob(NoOpJob("fourth")))

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(decision2, job3)
            decision2.decisionBefore = RefSet(decision)
            decision2.decisionAfter = RefSet(job, job2)
            job3.decisionBefore = RefSet(decision)
            job.decisionBefore = RefSet(decision2)
            job2.decisionBefore = RefSet(decision2)
            job.decisionAfter = RefSet(fourth)
            job2.decisionAfter = RefSet(fourth)
            job3.decisionAfter = RefSet(fourth)
            fourth.decisionBefore = RefSet(job, job2, job3)

        }

        "work with duplicate nodes" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val fork = GraphNode("fork-second-second2", WorkflowFork)
            val second = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val third = GraphNode("second2", WorkflowJob(NoOpJob("second")))
            val join = GraphNode("join-second-second2", WorkflowJoin)

            first.after = RefSet(fork)
            fork.before = RefSet(first)
            fork.after = RefSet(second, third)
            second.before = RefSet(fork)
            second.after = RefSet(join)
            third.before = RefSet(fork)
            third.after = RefSet(join)
            join.before = RefSet(second, third)

            Flatten(DuplicateNodes).values.toSet must_== Set(first, second, third, fork, join)
        }

        "work with duplicate sub workflows" in {
            val begin = GraphNode("begin", WorkflowJob(NoOpJob("begin")))
            val a = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val b = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val c = GraphNode("third", WorkflowJob(NoOpJob("third")))
            val d = GraphNode("fourth", WorkflowJob(NoOpJob("fourth")))
            val middle = GraphNode("middle", WorkflowJob(NoOpJob("middle")))
            val a2 = GraphNode("first2", WorkflowJob(NoOpJob("first")))
            val b2 = GraphNode("second2", WorkflowJob(NoOpJob("second")))
            val c2 = GraphNode("third2", WorkflowJob(NoOpJob("third")))
            val d2 = GraphNode("fourth2", WorkflowJob(NoOpJob("fourth")))
            val end = GraphNode("end", WorkflowEnd)

            begin.after = RefSet(a)
            a.before = RefSet(begin)
            a.after = RefSet(b)
            b.before = RefSet(a)
            b.after = RefSet(c)
            c.before = RefSet(b)
            c.after = RefSet(d)
            d.before = RefSet(c)
            d.after = RefSet(middle)

            middle.before = RefSet(d)
            middle.after = RefSet(a2)

            a2.before = RefSet(middle)
            a2.after = RefSet(b2)
            b2.before = RefSet(a2)
            b2.after = RefSet(c2)
            c2.before = RefSet(b2)
            c2.after = RefSet(d2)
            d2.before = RefSet(c2)
            d2.after = RefSet(end)

            Flatten(DuplicateSubWorkflows).values.toSet must_== Set(begin, a, b, c, d, middle, a2, b2, c2, d2)
        }

        "work with syntactically sugared decision option" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-option", WorkflowDecision(List("option" -> Predicates.BooleanProperty("${doOption}"))))
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("option"), Some("${doOption}"))
            val second = GraphNode("second", WorkflowJob(NoOpJob("second")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(option, second)
            option.decisionBefore = RefSet(decision)
            option.decisionAfter = RefSet(second)
            second.decisionBefore = RefSet(decision, option)
            second.after = RefSet(end)

            Flatten(SugarOption).values.toSet must_== Set(first, option, second, decision)
        }

        "work with syntactically sugared decision with multiple nodes in option route" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-sub1", WorkflowDecision(List("sub1" -> Predicates.BooleanProperty("${doSubWf}"))))
            val sub1 = GraphNode("sub1", WorkflowJob(NoOpJob("sub1")), RefSet(), RefSet(), RefSet(), RefSet(), Some("sub1"), Some("${doSubWf}"))
            val sub2 = GraphNode("sub2", WorkflowJob(NoOpJob("sub2")))
            val sub3 = GraphNode("sub3", WorkflowJob(NoOpJob("sub3")))
            val second = GraphNode("second", WorkflowJob(NoOpJob("second")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(sub1, second)
            sub1.decisionBefore = RefSet(decision)
            sub1.after = RefSet(sub2)
            sub2.before = RefSet(sub1)
            sub2.after = RefSet(sub3)
            sub3.before = RefSet(sub2)
            sub3.decisionAfter = RefSet(second)
            second.decisionBefore = RefSet(decision, sub3)
            second.after = RefSet(end)

            Flatten(MoreComplexSugarOption).values.toSet must_== Set(first, sub1, sub2, sub3, second, decision)
        }

        "work with syntactically sugared decision and regular decision" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-decision-option", WorkflowDecision(List("route1" -> Predicates.AlwaysTrue)))
            val decision2 = GraphNode("decision-option", WorkflowDecision(List("option" -> Predicates.BooleanProperty("${doOption}"))), RefSet(), RefSet(), RefSet(), RefSet(), Some("route1"))
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("option"), Some("${doOption}"))
            val default = GraphNode("default", WorkflowJob(NoOpJob("default")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val default2 = GraphNode("default2", WorkflowJob(NoOpJob("default2")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(decision2, default2)
            decision2.decisionBefore = RefSet(decision)
            decision2.decisionAfter = RefSet(option, default)
            option.decisionBefore = RefSet(decision2)
            option.decisionAfter = RefSet(default)
            default.decisionBefore = RefSet(decision2, option)
            default.decisionAfter = RefSet(default2)
            default2.decisionBefore = RefSet(decision)
            default2.after = RefSet(end)

            Flatten(DecisionAndSugarOption).values.toSet must_== Set(first, option, decision, decision2, default, default2)
        }

        "work for node with custom error-to" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val errorOption = GraphNode("errorOption", WorkflowJob(NoOpJob("errorOption")))
            val second = GraphNode("second", WorkflowJob(NoOpJob("second")))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(second)
            first.errorTo = Some(errorOption)
            second.before = RefSet(first)
            second.decisionAfter = RefSet(end)
            errorOption.before = RefSet(first)
            errorOption.decisionAfter = RefSet(end)

            Flatten(CustomErrorTo).values.toSet must_== Set(first, errorOption, second)
        }

        "work with sugar option and optional sub workflow" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("first")))
            val decision = GraphNode("decision-option", WorkflowDecision(List("option" -> Predicates.BooleanProperty("${doOption}"))))
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("option"), Some("${doOption}"))
            val subWfFirst = GraphNode("start", WorkflowJob(NoOpJob("start")))
            val default = GraphNode("default", WorkflowJob(NoOpJob("default")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(decision)
            decision.before = RefSet(first)
            decision.decisionAfter = RefSet(option, default)
            option.decisionBefore = RefSet(decision)
            option.after = RefSet(subWfFirst)
            subWfFirst.before = RefSet(option)
            subWfFirst.decisionAfter = RefSet(default)
            default.decisionBefore = RefSet(decision, subWfFirst)
            default.after = RefSet(end)

            Flatten(SugarOptionWithSubWf).values.toSet must_== Set(first, decision, option, subWfFirst, default)
        }

        "work with sugar option from Start" in {
            val decision = GraphNode("decision-option", WorkflowDecision(List("option" -> Predicates.BooleanProperty("${doOption}"))))
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("option"), Some("${doOption}"))
            val default = GraphNode("default", WorkflowJob(NoOpJob("default")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val end = GraphNode("end", WorkflowEnd)

            decision.decisionAfter = RefSet(option, default)
            option.decisionBefore = RefSet(decision)
            option.decisionAfter = RefSet(default)
            default.decisionBefore = RefSet(decision, option)
            default.after = RefSet(end)

            Flatten(SugarOptionFromStart).values.toSet must_== Set(decision, option, default)
        }

        "work with nested wf with mult end nodes" in {
            val decision = GraphNode("decision-option", WorkflowDecision(List("option" -> Predicates.BooleanProperty("${doOption}"))))
            val option = GraphNode("option", WorkflowJob(NoOpJob("option")), RefSet(), RefSet(), RefSet(), RefSet(), Some("option"))
            val default = GraphNode("default", WorkflowJob(NoOpJob("default")), RefSet(), RefSet(), RefSet(), RefSet(), Some("default"))
            val last = GraphNode("last", WorkflowJob(NoOpJob("last")))
            val end = GraphNode("end", WorkflowEnd)

            decision.decisionAfter = RefSet(option, default)
            option.decisionBefore = RefSet(decision)
            option.decisionAfter = RefSet(last)
            default.decisionBefore = RefSet(decision)
            default.decisionAfter = RefSet(last)
            last.decisionBefore = RefSet(option, default)
            last.after = RefSet(end)

            Flatten(SubWfWithTwoEndNodes).values.toSet must_== Set(decision, option, default, last)
        }

        "give workflow with repaired names for disallowed characters" in {
            val first = GraphNode("first", WorkflowJob(NoOpJob("${first}")))
            val end = GraphNode("end", WorkflowEnd)

            first.after = RefSet(end)

            Flatten(DisallowedNames).values.toSet must_== Set(first)
        }
    }

    def EmptyWorkflow = {
        val end = End dependsOn Nil
        Workflow("empty", end)
    }

    def SingleWorkflow = {
        val start = NoOpJob("start") dependsOn Start
        val end = End dependsOn start
        Workflow("single", end)
    }

    def SimpleWorkflow = {
        val first = NoOpJob("first") dependsOn Start
        val second = NoOpJob("second") dependsOn first
        val third = NoOpJob("third") dependsOn second
        val fourth = NoOpJob("fourth") dependsOn third
        val end = End dependsOn fourth
        Workflow("simple", end)
    }

    def SimpleForkJoin = {
        val first = NoOpJob("first") dependsOn Start
        val secondA = NoOpJob("secondA") dependsOn first
        val secondB = NoOpJob("secondB") dependsOn first
        val end = End dependsOn (secondA, secondB)
        Workflow("simple-fork-join", end)
    }

    def SimpleSubWorkflow = {
        val first = NoOpJob("begin") dependsOn Start
        val subWf = SimpleWorkflow dependsOn first
        val third = NoOpJob("final") dependsOn subWf
        val end = End dependsOn third
        Workflow("simple-sub-workflow", end)
    }

    def TwoSimpleForkJoins = {
        val first = NoOpJob("first") dependsOn Start
        val secondA = NoOpJob("secondA") dependsOn first
        val secondB = NoOpJob("secondB") dependsOn first
        val third = NoOpJob("third") dependsOn (secondA, secondB)
        val fourthA = NoOpJob("fourthA") dependsOn third
        val fourthB = NoOpJob("fourthB") dependsOn third
        val end = End dependsOn (fourthA, fourthB)
        Workflow("two-simple-fork-joins", end)
    }

    def SubworkflowWithForkJoins = {
        val start = NoOpJob("start") dependsOn Start
        val sub = SimpleWorkflow dependsOn start
        val thirdA = NoOpJob("thirdA") dependsOn sub
        val thirdB = NoOpJob("thirdB") dependsOn sub
        val end = End dependsOn (thirdA, thirdB)
        Workflow("sub-fork-join", end)
    }

    def NestedForkJoin = {
        val first = NoOpJob("first") dependsOn Start
        val secondA = NoOpJob("secondA") dependsOn first
        val secondB = NoOpJob("secondB") dependsOn first
        val thirdA = NoOpJob("thirdA") dependsOn secondA
        val thirdB = NoOpJob("thirdB") dependsOn secondA
        val thirdC = NoOpJob("thirdC") dependsOn secondB
        val fourth = NoOpJob("fourth") dependsOn (thirdA, thirdB, thirdC)
        Workflow("nested-fork-join", fourth)
    }

    def SimpleDecision = {
        val first = NoOpJob("first") dependsOn Start
        val decision = Decision("route1" -> Predicates.AlwaysTrue) dependsOn first //decision is a DecisionNode
        val default = NoOpJob("default") dependsOn (decision default)
        val option = NoOpJob("option") dependsOn (decision option "route1")
        val second = NoOpJob("second") dependsOn OneOf(default, option)
        val done = End dependsOn second
        Workflow("simple-decision", done)
    }

    def MoreComplexDecision = {
        val first = NoOpJob("first") dependsOn Start
        val decision = Decision("route1" -> Predicates.AlwaysTrue) dependsOn first
        val defaultRoute = {
            val default = NoOpJob("default") dependsOn (decision default)
            val default2 = NoOpJob("default2") dependsOn default
            default2
        }
        val option = NoOpJob("option") dependsOn (decision option "route1")
        val second = NoOpJob("second") dependsOn OneOf(defaultRoute, option)
        val done = End dependsOn second
        Workflow("more-complex-decision", done)
    }
    def MultipleDecision = {
        val first = NoOpJob("first") dependsOn Start
        val dec = Decision(
            "route1" -> Predicates.AlwaysTrue
        ) dependsOn first
        val dec2 = Decision(
            "route1" -> Predicates.AlwaysTrue
        ) dependsOn (dec default)
        val job = NoOpJob("job") dependsOn (dec2 default)
        val job2 = NoOpJob("job2") dependsOn (dec2 option "route1")
        val job3 = NoOpJob("job3") dependsOn (dec option "route1")
        val fourth = NoOpJob("fourth") dependsOn OneOf(job, job2, job3)
        val end = End dependsOn fourth
        Workflow("test", end)
    }

    def ForkJoinOnly = {
        val startA = NoOpJob("startA") dependsOn Start
        val startB = NoOpJob("startB") dependsOn Start
        val end = End dependsOn (startA, startB)
        Workflow("fork-join-only", end)
    }

    def DuplicateNodes = {
        val first = NoOpJob("first") dependsOn Start
        val second = NoOpJob("second") dependsOn first
        val third = NoOpJob("second") dependsOn first
        val end = End dependsOn (second, third)
        Workflow("duplicate-nodes", end)
    }

    def DuplicateSubWorkflows = {
        val begin = NoOpJob("begin") dependsOn Start
        val sub1 = SimpleWorkflow dependsOn begin
        val middle = NoOpJob("middle") dependsOn sub1
        val sub2 = SimpleWorkflow dependsOn middle
        val end = End dependsOn sub2
        Workflow("duplicate-sub-workflows", end)
    }

    def SugarOption = {
        val first = NoOpJob("first") dependsOn Start
        val option = NoOpJob("option") dependsOn first doIf "doOption"
        val second = NoOpJob("second") dependsOn (first andOptionally_: option)
        val done = End dependsOn second
        Workflow("sugar-option-decision", done)
    }

    def MoreComplexSugarOption = {
        val first = NoOpJob("first") dependsOn Start
        val sub1 = NoOpJob("sub1") dependsOn first doIf "doSubWf"
        val sub2 = NoOpJob("sub2") dependsOn sub1
        val sub3 = NoOpJob("sub3") dependsOn sub2
        val second = NoOpJob("second") dependsOn (first andOptionally_: sub3)
        val end = End dependsOn second
        Workflow("more-complex-sugar-decision", end)
    }

    def DecisionAndSugarOption = {
        val first = NoOpJob("first") dependsOn Start
        val decision = Decision(
            "route1" -> Predicates.AlwaysTrue
        ) dependsOn first
        val option = NoOpJob("option") dependsOn (decision option "route1") doIf "doOption"
        val default = NoOpJob("default") dependsOn ((decision option "route1") andOptionally_: option)
        val default2 = NoOpJob("default2") dependsOn OneOf(decision default, default)
        val end = End dependsOn default2
        Workflow("mixed-decision-styles", end)
    }

    def CustomErrorTo = {
        val first = NoOpJob("first") dependsOn Start
        val errorOption = NoOpJob("errorOption") dependsOn (first error)
        val second = NoOpJob("second") dependsOn first
        val end = End dependsOn OneOf(second, errorOption)
        Workflow("custom-errorTo", end)
    }

    def SugarOptionWithSubWf = {
        val first = NoOpJob("first") dependsOn Start
        val option = NoOpJob("option") dependsOn first doIf "doOption"
        val optionalWf = SingleWorkflow dependsOn option
        val default = NoOpJob("default") dependsOn (first andOptionally_: optionalWf)
        val end = End dependsOn default
        Workflow("sugar-option-with-sub-wf", end)
    }

    def SugarOptionFromStart = {
        val option = NoOpJob("option") dependsOn Start doIf "doOption"
        val default = NoOpJob("default") dependsOn (Start andOptionally_: option)
        val end = End dependsOn default
        Workflow("sugar-option-from-start", end)
    }

    def SubWfWithTwoEndNodes = {
        val wf = WfWithTwoEndNodes dependsOn Start
        val last = NoOpJob("last") dependsOn wf
        val end = End dependsOn last
        Workflow("nested-wf-with-mult-end-nodes", end)
    }

    def WfWithTwoEndNodes = {
        val decision = Decision(
            "option" -> Predicates.BooleanProperty("${doOption}")
        ) dependsOn Start
        val option = NoOpJob("option") dependsOn (decision option "option")
        val default = NoOpJob("default") dependsOn (decision default)
        val end = End dependsOn OneOf(option, default)
        Workflow("wf-with-mult-end-nodes", end)
    }

    def DisallowedNames = {
        val first = NoOpJob("${first}") dependsOn Start
        val end = End dependsOn first
        Workflow("disallowed-names", end)
    }
}
