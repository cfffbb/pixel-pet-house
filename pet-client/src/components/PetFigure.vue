<template>
  <div class="pet-figure" :style="{ width: size + 'px', height: size + 'px' }">
    <img
      v-if="sampleImg && !imgError"
      :src="sampleImg"
      :style="{ width: size + 'px', height: size + 'px' }"
      class="pet-sample-img"
      alt="宠物"
      @error="imgError = true"
    />
    <svg v-else :width="size" :height="size" viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
      <!-- 耳朵/角(按种类,老年略垂) -->
      <template v-if="family === 'cat'">
        <path d="M32 34 L22 6 L50 26 Z" :fill="dark" :transform="elder ? 'rotate(8 32 34)' : ''" />
        <path d="M88 34 L98 6 L70 26 Z" :fill="dark" :transform="elder ? 'rotate(-8 88 34)' : ''" />
      </template>
      <template v-else-if="family === 'dog'">
        <ellipse cx="27" cy="30" rx="11" ry="20" :fill="dark" transform="rotate(-22 27 30)" />
        <ellipse cx="93" cy="30" rx="11" ry="20" :fill="dark" transform="rotate(22 93 30)" />
      </template>
      <template v-else-if="family === 'rabbit'">
        <ellipse cx="43" cy="16" rx="9" ry="23" :fill="color" :transform="elder ? 'rotate(10 43 16)' : ''" />
        <ellipse cx="77" cy="16" rx="9" ry="23" :fill="color" :transform="elder ? 'rotate(-10 77 16)' : ''" />
        <ellipse cx="43" cy="18" rx="4.5" ry="14" :fill="inner" />
        <ellipse cx="77" cy="18" rx="4.5" ry="14" :fill="inner" />
      </template>
      <template v-else-if="family === 'dragon'">
        <path d="M30 40 L14 10 L44 28 Z" :fill="dark" />
        <path d="M90 40 L106 10 L76 28 Z" :fill="dark" />
      </template>
      <template v-else>
        <ellipse cx="35" cy="27" rx="9" ry="12" :fill="dark" />
        <ellipse cx="85" cy="27" rx="9" ry="12" :fill="dark" />
      </template>

      <!-- 身体 + 头 -->
      <ellipse cx="60" :cy="baby ? 90 : 82" :rx="baby ? 26 : 38" :ry="baby ? 18 : 27" :fill="color" />
      <circle cx="60" :cy="baby ? 50 : 48" :r="baby ? 36 : 30" :fill="color" />

      <!-- 脸 -->
      <circle cx="50" cy="46" :r="baby ? 5.6 : 4.6" fill="#4a3f35" />
      <circle cx="70" cy="46" :r="baby ? 5.6 : 4.6" fill="#4a3f35" />
      <circle cx="52" cy="43.5" r="1.7" fill="#fff" />
      <circle cx="72" cy="43.5" r="1.7" fill="#fff" />
      <path d="M55 56 Q60 62 65 56" stroke="#4a3f35" stroke-width="2.6" fill="none" stroke-linecap="round" />
      <circle cx="42" cy="55" r="5" fill="#f2a2b5" opacity="0.55" />
      <circle cx="78" cy="55" r="5" fill="#f2a2b5" opacity="0.55" />

      <!-- 老年灰化 -->
      <g v-if="elder">
        <circle cx="60" cy="48" r="30" fill="#9a9a9a" opacity="0.3" />
        <ellipse cx="60" cy="82" rx="38" ry="27" fill="#9a9a9a" opacity="0.3" />
      </g>

      <!-- 母:蝴蝶结 -->
      <g v-if="gender === 'FEMALE'" transform="translate(84 18)">
        <path d="M0 0 L-13 -8 L-13 8 Z" fill="#f77fb0" />
        <path d="M0 0 L13 -8 L13 8 Z" fill="#f77fb0" />
        <circle cx="0" cy="0" r="3.6" fill="#e85a95" />
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { petSampleUrl } from '../petSamples';

const props = defineProps({
  typeCode: { type: String, default: 'CAT' },
  gender: { type: String, default: 'MALE' },
  size: { type: Number, default: 64 },
  stage: { type: String, default: 'ADULT' },
});

const imgError = ref(false);

const sampleImg = computed(() => {
  const url = petSampleUrl(props.typeCode, props.gender);
  return url;
});

const COLORS = {
  RAT: '#b8b0a8', OX: '#a97b50', TIGER: '#e8a33d', RABBIT: '#f5e6e8',
  DRAGON: '#7cc47c', SNAKE: '#8fbf6f', HORSE: '#b0835c', GOAT: '#e8e0d0',
  MONKEY: '#c08a5a', ROOSTER: '#e07b54', DOG: '#e8b34d', PIG: '#f2a2b5', CAT: '#f0a35c',
};
const DARKS = {
  RAT: '#8f8579', OX: '#7d5a38', TIGER: '#c9832a', RABBIT: '#e5c9cf',
  DRAGON: '#5aa05a', SNAKE: '#6f9f55', HORSE: '#8a6342', GOAT: '#cbbfa8',
  MONKEY: '#9c6a42', ROOSTER: '#b75a38', DOG: '#c99235', PIG: '#e0809a', CAT: '#d48142',
};

const color = computed(() => COLORS[props.typeCode] || '#f0a35c');
const dark = computed(() => DARKS[props.typeCode] || '#d48142');
const inner = computed(() => (props.typeCode === 'RABBIT' ? '#f0b8c8' : '#ffffff'));
const family = computed(
  () => ({ CAT: 'cat', DOG: 'dog', RABBIT: 'rabbit', DRAGON: 'dragon', SNAKE: 'none' }[props.typeCode] || 'round')
);
const baby = computed(() => props.stage === 'BABY');
const elder = computed(() => props.stage === 'ELDER');
</script>

<style scoped>
.pet-figure { display: inline-block; }
.pet-sample-img {
  image-rendering: auto;
  object-fit: contain;
  filter: drop-shadow(0 3px 0 rgba(60, 40, 20, 0.16));
}
</style>
