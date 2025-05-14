// ScreenCapture.cpp
#include <windows.h>
#include <d3d11.h>
#include <dxgi1_2.h>
#include <jni.h>
#include "com_example_snaprec_NativeScreenCapture.h"
#include <vector>
#include <wrl/client.h>
#include <cstdint>

using namespace Microsoft::WRL;

static ComPtr<ID3D11Device> g_device;
static ComPtr<ID3D11DeviceContext> g_context;
static ComPtr<IDXGIOutputDuplication> g_duplication;
static int g_width = 1920;
static int g_height = 1080;

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_example_snaprec_NativeScreenCapture_captureFrame(JNIEnv* env, jobject) {
    if (!g_duplication) {
        // 初始化 D3D11 和 Duplication API
        D3D_FEATURE_LEVEL fl;
        D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, 0,
                          nullptr, 0, D3D11_SDK_VERSION, &g_device, &fl, &g_context);

        ComPtr<IDXGIDevice> dxgiDevice;
        g_device.As(&dxgiDevice);
        ComPtr<IDXGIAdapter> adapter;
        dxgiDevice->GetAdapter(&adapter);
        ComPtr<IDXGIOutput> output;
        adapter->EnumOutputs(0, &output);
        ComPtr<IDXGIOutput1> output1;
        output.As(&output1);
        output1->DuplicateOutput(g_device.Get(), &g_duplication);
    }

    DXGI_OUTDUPL_FRAME_INFO frameInfo = {};
    ComPtr<IDXGIResource> desktopResource;
    HRESULT hr = g_duplication->AcquireNextFrame(100, &frameInfo, &desktopResource);
    if (FAILED(hr)) return env->NewByteArray(0);

    ComPtr<ID3D11Texture2D> frameTex;
    desktopResource.As(&frameTex);

    // Create staging texture
    D3D11_TEXTURE2D_DESC desc;
    frameTex->GetDesc(&desc);
    desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
    desc.Usage = D3D11_USAGE_STAGING;
    desc.BindFlags = 0;
    desc.MiscFlags = 0;

    ComPtr<ID3D11Texture2D> stagingTex;
    g_device->CreateTexture2D(&desc, nullptr, &stagingTex);
    g_context->CopyResource(stagingTex.Get(), frameTex.Get());

    D3D11_MAPPED_SUBRESOURCE mapped = {};
    g_context->Map(stagingTex.Get(), 0, D3D11_MAP_READ, 0, &mapped);

    int pitch = desc.Width * 4; // 原本是 BGRA
    std::vector<jbyte> frame(desc.Width * desc.Height * 3);

    for (UINT y = 0; y < desc.Height; ++y) {
        const uint8_t* src = (uint8_t*)mapped.pData + y * mapped.RowPitch;
        for (UINT x = 0; x < desc.Width; ++x) {
            frame[(y * desc.Width + x) * 3 + 0] = src[x * 4 + 0]; // B
            frame[(y * desc.Width + x) * 3 + 1] = src[x * 4 + 1]; // G
            frame[(y * desc.Width + x) * 3 + 2] = src[x * 4 + 2]; // R
        }
    }

    g_context->Unmap(stagingTex.Get(), 0);
    g_duplication->ReleaseFrame();

    jbyteArray result = env->NewByteArray(frame.size());
    env->SetByteArrayRegion(result, 0, frame.size(), frame.data());
    return result;
}

//x86_64-w64-mingw32-g++ -I"C:\Program Files\Java\jdk-21\include" -I"C:\Program Files\Java\jdk-21\include\win32" -shared -o ScreenCapture.dll ScreenCapture.cpp -ld3d11 -ldxgi
