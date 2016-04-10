package org.stagemonitor.requestmonitor.profiler;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.stagemonitor.core.instrument.StagemonitorByteBuddyTransformer;
import org.stagemonitor.core.instrument.StagemonitorClassNameMatcher;

public class ProfilingTransformer extends StagemonitorByteBuddyTransformer {

	@Override
	public ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
		return nameStartsWith(Profiler.class.getPackage().getName())
				.or(makeSureClassesAreNotInstrumentedTwice());
	}

	/*
	 * If this is a subclass of ProfilingTransformer, make sure to not instrument classes
	 * which are matched by ProfilingTransformer
	 */
	private ElementMatcher.Junction<TypeDescription> makeSureClassesAreNotInstrumentedTwice() {
		return isSubclass() ? new StagemonitorClassNameMatcher() : ElementMatchers.<TypeDescription>none();
	}

	private boolean isSubclass() {
		return getClass() != ProfilingTransformer.class;
	}

	@Override
	public AgentBuilder.Transformer getTransformer() {
		return new AgentBuilder.Transformer() {
			@Override
			public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
				return builder
						.visit(Advice.to(ProfilingTransformer.class)
								.on(not(isConstructor())
										.and(not(isAbstract()))
										.and(not(isNative()))
										.and(not(isFinal()))
										.and(not(isTypeInitializer()))
										.and(not(nameContains("access$")))
										.and(getExtraElementMatchers())
								));
			}
		};
	}

	protected ElementMatcher<? super MethodDescription> getExtraElementMatchers() {
		return any();
	}

	@Advice.OnMethodEnter
	public static void enter(@Advice.Origin("#t.#m#d") String signature) {
		Profiler.start(signature);
	}

	@Advice.OnMethodExit
	public static void exit() {
		Profiler.stop();
	}

}
