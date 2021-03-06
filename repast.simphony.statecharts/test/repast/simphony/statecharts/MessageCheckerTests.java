package repast.simphony.statecharts;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import repast.simphony.parameter.Parameters;
import simphony.util.messages.MessageCenter;
import simphony.util.messages.MessageEvent;
import simphony.util.messages.MessageEventListener;

public class MessageCheckerTests {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void unconditionalMessageCheckerTest() {
		UnconditionalMessageChecker umc = new UnconditionalMessageChecker();
		assertEquals(true, umc.checkMessage("hello"));
		assertEquals(false, umc.checkMessage(null));
		int msg = 6;
		assertEquals(true, umc.checkMessage(msg));
	}

	// Test classes
	private static interface A {
	}

	private static class B implements A {
	}

	private static class C extends B {
	}

	@Test
	public void unconditionalByMessageCheckerTest() {
		UnconditionalByClassMessageChecker ubcmc = new UnconditionalByClassMessageChecker(
				String.class);
		assertEquals(true, ubcmc.checkMessage("hello"));
		assertEquals(false, ubcmc.checkMessage(1));
		assertEquals(false, ubcmc.checkMessage(new Object()));
		assertEquals(false, ubcmc.checkMessage(null));

		UnconditionalByClassMessageChecker ubcmc2 = new UnconditionalByClassMessageChecker(
				A.class);
		assertEquals(true, ubcmc2.checkMessage(new A() {
		}));
		assertEquals(true, ubcmc2.checkMessage(new B()));
		assertEquals(true, ubcmc2.checkMessage(new C()));

		UnconditionalByClassMessageChecker ubcmc3 = new UnconditionalByClassMessageChecker(
				C.class);
		assertEquals(false, ubcmc3.checkMessage(new B()));
	}

	// Test class
	private static class D {
		private int d;

		public D(int d) {
			this.d = d;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + d;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			D other = (D) obj;
			if (d != other.d)
				return false;
			return true;
		}
	}

	@Test
	public void messageEqualsMessageCheckerTest() {
		MessageEqualsMessageChecker<Object,String> mc1 = new MessageEqualsMessageChecker<Object,String>(
				"hello",String.class);
		mc1.setAgent(new Object());
		assertEquals(true, mc1.checkMessage("hello"));
		assertEquals(false, mc1.checkMessage("hell"));
		assertEquals(false, mc1.checkMessage(1));
		assertEquals(false, mc1.checkMessage(new Object()));
		assertEquals(false, mc1.checkMessage(new B()));

		C c = new C();
		MessageEqualsMessageChecker<Object,C> mc2 = new MessageEqualsMessageChecker<Object,C>(
				c,C.class);
		mc2.setAgent(new Object());
		assertEquals(true, mc2.checkMessage(c));
		assertEquals(false, mc2.checkMessage(new C()));

		D d = new D(3);
		MessageEqualsMessageChecker<Object,D> mc3 = new MessageEqualsMessageChecker<Object,D>(
				d,D.class);
		mc3.setAgent(new Object());
		assertEquals(true, mc3.checkMessage(new D(3)));
		assertEquals(false, mc3.checkMessage(new D(2)));
	}

	@Test
	public void messageConditionMessageCheckerTest() {
		MessageConditionMessageChecker<Object, D> mc1 = new MessageConditionMessageChecker<Object, D>(
				new MessageCondition<Object, D>() {

					@Override
					public boolean isTrue(Object agent,
							Transition<Object> transition, D message, Parameters params) throws Exception {

						return message.d == 3 ? true : false;

					}

				}, D.class);

		mc1.setAgent(new Object());
		assertEquals(true, mc1.checkMessage(new D(3)));
		assertEquals(false, mc1.checkMessage(new D(4)));
		assertEquals(false, mc1.checkMessage("hell"));

		MessageConditionMessageChecker<Object,Object> mc2 = new MessageConditionMessageChecker<Object,Object>(
				new MessageCondition<Object,Object>() {

					@Override
					public boolean isTrue(Object agent,
							Transition<Object> transition, Object message, Parameters params) throws Exception {
						throw new Exception();
					}

					@Override
					public String toString() {
						return "Exception throwing MessageCondition";
					}

				},Object.class);
		mc2.setAgent(new Object());// Only for testing purposes

		class MyMessageEventListener implements MessageEventListener {

			public boolean messageReceived = false;
			public String message;

			@Override
			public void messageReceived(MessageEvent me) {
				messageReceived = true;
				message = (String) me.getMessage();
			}

		}

		MyMessageEventListener mel = new MyMessageEventListener();
		MessageCenter.addMessageListener(mel);
		assertEquals(false, mel.messageReceived);
		assertEquals(false, mc2.checkMessage("hello"));
		assertEquals(true, mel.messageReceived);
		assertEquals(
				"Error encountered when calling message condition in: MessageConditionMessageChecker with Exception throwing MessageCondition",
				mel.message);

	}

}
