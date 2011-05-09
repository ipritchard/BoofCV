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

package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.FastCornerIntensity;
import gecv.misc.DiscretizedCircle;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * An implementation of {@link FastCornerIntensity} algorithm that is designed to be
 * more easily read and verified for correctness.
 * <p/>
 *
 * @author Peter Abeles
 */
public class FastCorner12_I8 implements FastCornerIntensity<ImageUInt8> {

	private int minCont;
	private final static int radius = 3;

	// how similar do the pixel in the circle need to be to the center pixel
	private int pixelTol;

	// corner intensity image
	private ImageFloat32 featureIntensity;

	// list of pixels that might be corners.
	private QueueCorner candidates;

	/**
	 * @param imgWidth Input image width.
	 * @param imgHeight input image height.
	 * @param pixelTol The difference in intensity value from the center pixel the circle needs to be.
	 * @param minCont  The minimum number of continuous pixels that a circle needs to be a corner.
	 */
	public FastCorner12_I8( int imgWidth , int imgHeight , int pixelTol, int minCont) {
		this.pixelTol = pixelTol;
		this.minCont = minCont;

		featureIntensity = new ImageFloat32(imgWidth, imgHeight);

		candidates = new QueueCorner(imgWidth);
	}

	@Override
	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}

	@Override
	public QueueCorner getCandidates() {
		return candidates;
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public void process( ImageUInt8 img ) {
		candidates.reset();
		final byte[] data = img.data;

		final int width = img.getWidth();
		final int yEnd = img.getHeight() - radius;
		final int stride = img.stride;

		// relative offsets of pixel locations in a circle
		int []offsets = DiscretizedCircle.imageOffsets(radius, stride);

		final float[] inten = featureIntensity.data;

		int offA = offsets[0];
		int offB = offsets[4];
		int offC = offsets[8];
		int offD = offsets[12];

		for (int y = radius; y < yEnd; y++) {
			int rowStart = img.startIndex + stride * y;
			int endX = rowStart + width - radius;
			int intenIndex = featureIntensity.startIndex + y*featureIntensity.stride+radius;
			for (int index = rowStart + radius; index < endX; index++,intenIndex++) {

				// quickly eliminate bad choices by examining 4 points spread out
				int center = data[index] & 0xFF;

				int a = data[index + offA] & 0xFF;
				int b = data[index + offB] & 0xFF;
				int c = data[index + offC] & 0xFF;
				int d = data[index + offD] & 0xFF;

				int thresh = center - pixelTol;

				int action = 0;

				// check to see if it is significantly below the center pixel
				if (a < thresh && c < thresh) {
					if (b < thresh) {
						action = -1;
					} else if (d < thresh) {
						action = -1;
					}
				} else if (b < thresh && d < thresh) {
					if (a < thresh) {
						action = -1;
					} else if (c < thresh) {
						action = -1;
					}
				} else {
					// see if it is significantly more than the center pixel
					thresh = center + pixelTol;

					if (a > thresh && c > thresh) {
						if (d > thresh) {
							action = 1;
						} else if (b > thresh) {
							action = 1;
						}
					}
					if (b > thresh && d > thresh) {
						if (a > thresh) {
							action = 1;
						} else if (c > thresh) {
							action = 1;
						}
					}
				}

				// can't be a corner here so just continue to the next pixel
				if (action == 0) {
					inten[intenIndex] = 0F;
					continue;
				}

				boolean isCorner = false;

				// move until it finds a valid pixel
				int totalDiff = 0;

				// see if the first pixel is valid or not
				int val = a - center;
				if ((action == -1 && val < -pixelTol) || val > pixelTol) {
					// if it is valid then it needs to deal with wrapping
					int i;
					// find the point a bad pixel is found
					totalDiff += val;
					for (i = 1; i < offsets.length; i++) {
						val = (data[index + offsets[i]] & 0xFF) - center;

						if (action == -1) {
							if (val >= -pixelTol) break;
						} else if (val <= pixelTol) break;

						totalDiff += val;
					}

					int frontLength = i;

					if (frontLength < minCont) {
						// go the other direction
						for (i = offsets.length - 1; i >= 0; i--) {
							val = (data[index + offsets[i]] & 0xFF) - center;

							if (action == -1) {
								if (val >= -pixelTol) break;
							} else if (val <= pixelTol) break;
							totalDiff += val;
						}
						if (offsets.length - 1 - i + frontLength >= minCont) {
							isCorner = true;
						}
					} else {
						isCorner = true;
					}

				} else {
					// find the first good pixel
					int start;
					for (start = 0; start < offsets.length; start++) {
						val = (data[index + offsets[start]] & 0xFF) - center;

						if (action == -1) {
							if (val < -pixelTol) break;
						} else if (val > pixelTol) break;
					}

					// find the point where the good pixels stop
					int stop;
					for (stop = start + 1; stop < offsets.length; stop++) {
						val = (data[index + offsets[stop]] & 0xFF) - center;

						if (action == -1) {
							if (val >= -pixelTol) break;
						} else if (val <= pixelTol) break;
						totalDiff += val;
					}

					isCorner = stop - start >= minCont;

				}

				if (isCorner) {
					inten[intenIndex] = action == -1 ? -totalDiff : totalDiff;
					// declare room for more features
					if( candidates.isFull() ) {
						candidates.resize(candidates.getMaxSize()*2);
					}
					candidates.add( index-rowStart , y );
				} else {
					inten[intenIndex] = 0F;
				}
			}
		}
	}
}
