/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.derivative.sobel;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGradientEdge_Naive {

	Random rand = new Random(0xfeed);

	private final int width = 4;
	private final int height = 5;

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void compareToKnown_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, rand);

		ImageSInt16 derivX = new ImageSInt16(width, height);
		ImageSInt16 derivY = new ImageSInt16(width, height);

		GecvTesting.checkSubImage(this, "compareToKnown_I8", true, img, derivX, derivY);
	}

	public void compareToKnown_I8(ImageUInt8 img, ImageSInt16 derivX, ImageSInt16 derivY) {
		GradientSobel_Naive.process(img, derivX, derivY);

		int dX = -((img.get(0, 2) + img.get(0, 0)) + img.get(0, 1) * 2);
		dX += (img.get(2, 2) + img.get(2, 0)) + img.get(2, 1) * 2;

		int dY = -((img.get(2, 0) + img.get(0, 0)) + img.get(1, 0) * 2);
		dY += (img.get(2, 2) + img.get(0, 2)) + img.get(1, 2) * 2;

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void compareToKnown_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 255);

		ImageFloat32 derivX = new ImageFloat32(width, height);
		ImageFloat32 derivY = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "compareToKnown_F32", true, img, derivX, derivY);

	}

	public void compareToKnown_F32(ImageFloat32 img, ImageFloat32 derivX, ImageFloat32 derivY) {
		GradientSobel_Naive.process(img, derivX, derivY);

		float dX = -((img.get(0, 2) + img.get(0, 0)) * 0.25f + img.get(0, 1) * 0.5f);
		dX += (img.get(2, 2) + img.get(2, 0)) * 0.25f + img.get(2, 1) * 0.5f;

		float dY = -((img.get(2, 0) + img.get(0, 0)) * 0.25f + img.get(1, 0) * 0.5f);
		dY += (img.get(2, 2) + img.get(0, 2)) * 0.25f + img.get(1, 2) * 0.5f;

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}
}
