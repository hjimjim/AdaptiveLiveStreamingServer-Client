package com.piggy.client.player;
import static com.piggy.client.decoder.H264Context.NAL_AUD;
import static com.piggy.client.decoder.H264Context.NAL_IDR_SLICE;
import static com.piggy.client.decoder.H264Context.NAL_SLICE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.Arrays;

import javax.swing.JFrame;

import com.piggy.client.decoder.AVFrame;
import com.piggy.client.decoder.AVPacket;
import com.piggy.client.decoder.H264Decoder;
import com.piggy.client.decoder.MpegEncContext;
import com.piggy.client.util.FrameUtils;

public class H264Player implements Runnable {
	public static final int INBUF_SIZE = 65535;
	private int[] buffer = null;
	boolean foundFrameStart;
	static final boolean debug = false;

    H264Decoder codec;
    MpegEncContext c= null;

    int frame, len;
    int[] got_picture = new int[1];
    AVFrame picture;

    byte[] inbuf = new byte[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
	int[] inbuf_int = new int[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
    AVPacket avpkt;

    PipedInputStream bin;
    View v;

	public H264Player(PipedInputStream pipedInputStream, View v) {
        this.bin = pipedInputStream;
		this.v = v;
        init();
	}

    private void init() {
        avpkt = new AVPacket();
        avpkt.av_init_packet();

	    /* set end of buffer to 0 (this ensures that no overreading happens for damaged mpeg streams) */
        Arrays.fill(inbuf, INBUF_SIZE, MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, (byte)0);

        codec = new H264Decoder();
        if (codec == null) {
            System.out.println("codec not found\n");
            System.exit(1);
        } // if

        c= MpegEncContext.avcodec_alloc_context();
        picture= AVFrame.avcodec_alloc_frame();

        if((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED)!=0)
            c.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

	    /* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

	    /* open it */
        if (c.avcodec_open(codec) < 0) {
            System.out.println("could not open codec\n");
            System.exit(1);
        }
    }

    public void replay() {
        stop();
        init();
    }
    public void run() {
		System.out.println("Playing ");
        while(true) {
            try {
                if(bin.available() > 0) {
                    playFile(bin);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

	public void stop() {
		try {
			c.avcodec_close();
            c = null;
            picture = null;
            System.out.println("Stop playing video.");
			//bin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isEndOfFrame(int code) {
		int nal = code & 0x1F;

		if (nal == NAL_AUD) {
			foundFrameStart = false;
			return true;
		}

		boolean foundFrame = foundFrameStart;
		if (nal == NAL_SLICE || nal == NAL_IDR_SLICE) {
			if (foundFrameStart) {
				return true;
			}
			foundFrameStart = true;
		} else {
			foundFrameStart = false;
		}

		return foundFrame;
	}

	public int playFile(PipedInputStream bin) {
		System.out.println("Video decoding\n");
        try {
        /* the codec gives us the frame size, in samples */

            frame = 0;
            int dataPointer;
            int fileOffset = 0;
            foundFrameStart = false;
            // avpkt must contain exactly 1 NAL Unit in order for decoder to decode correctly.
            // thus we must read until we get next NAL header before sending it to decoder.
            // Find 1st NAL


            int[] cacheRead = new int[5];
            cacheRead[0] = bin.read();
            cacheRead[1] = bin.read();
            cacheRead[2] = bin.read();
            cacheRead[3] = bin.read();


            while (!(
                    cacheRead[0] == 0x00 &&
                            cacheRead[1] == 0x00 &&
                            cacheRead[2] == 0x00 &&
                            cacheRead[3] == 0x01
            )) {
                cacheRead[0] = cacheRead[1];
                cacheRead[1] = cacheRead[2];
                cacheRead[2] = cacheRead[3];
                cacheRead[3] = bin.read();
            } // while

            boolean hasMoreNAL = true;
            cacheRead[4] = bin.read();

            // 4 first bytes always indicate NAL header
            while (hasMoreNAL) {
                inbuf_int[0] = cacheRead[0];
                inbuf_int[1] = cacheRead[1];
                inbuf_int[2] = cacheRead[2];
                inbuf_int[3] = cacheRead[3];
                inbuf_int[4] = cacheRead[4];

                dataPointer = 5;
                // Find next NAL
                cacheRead[0] = bin.read();
                if (cacheRead[0] == -1) hasMoreNAL = false;
                cacheRead[1] = bin.read();
                if (cacheRead[1] == -1) hasMoreNAL = false;
                cacheRead[2] = bin.read();
                if (cacheRead[2] == -1) hasMoreNAL = false;
                cacheRead[3] = bin.read();
                if (cacheRead[3] == -1) hasMoreNAL = false;
                cacheRead[4] = bin.read();
                if (cacheRead[4] == -1) hasMoreNAL = false;
                while (!(
                        cacheRead[0] == 0x00 &&
                                cacheRead[1] == 0x00 &&
                                cacheRead[2] == 0x00 &&
                                cacheRead[3] == 0x01 &&
                                isEndOfFrame(cacheRead[4])
                ) && hasMoreNAL) {
                    inbuf_int[dataPointer++] = cacheRead[0];
                    cacheRead[0] = cacheRead[1];
                    cacheRead[1] = cacheRead[2];
                    cacheRead[2] = cacheRead[3];
                    cacheRead[3] = cacheRead[4];
                    cacheRead[4] = bin.read();
                    if (cacheRead[4] == -1) hasMoreNAL = false;
                } // while

                avpkt.size = dataPointer;
                if (debug) {
                    System.out.println(String.format("Offset 0x%X, packet size 0x%X, nal=0x%X", fileOffset, dataPointer, inbuf_int[4] & 0x1F));
                }
                fileOffset += dataPointer;
                avpkt.data_base = inbuf_int;
                avpkt.data_offset = 0;
                try {
                    while (avpkt.size > 0) {
                        len = c.avcodec_decode_video2(picture, got_picture, avpkt);
                        if (len < 0) {
                            System.out.println("Error while decoding frame " + frame);
                            // Discard current packet and proceed to next packet
                            break;
                        } // if
                        if (got_picture[0] != 0) {
                            picture = c.priv_data.displayPicture;

                            int imageWidth = picture.imageWidthWOEdge;
                            int imageHeight = picture.imageHeightWOEdge;
                            int bufferSize = imageWidth * imageHeight;
                            if (buffer == null || bufferSize != buffer.length) {
                                buffer = new int[bufferSize];
                            }
                            FrameUtils.YUV2RGB_WOEdge(picture, buffer);
                            v.displayPanel.lastFrame = v.displayPanel.createImage(new MemoryImageSource(imageWidth
                                    , imageHeight, buffer, 0, imageWidth));
                            v.displayPanel.invalidate();
                            v.displayPanel.updateUI();
                        }
                        avpkt.size -= len;
                        avpkt.data_offset += len;
                    }
                } catch (Exception ie) {
                    // Any exception, we should try to proceed reading next packet!
                    ie.printStackTrace();
                } // try
            } // while
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
            } catch (Exception ee) {
            }
        } // try
        return 1;
    }
}
