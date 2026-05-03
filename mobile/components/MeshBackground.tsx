import React, { useMemo, useEffect } from 'react';
import { StyleSheet, View, useWindowDimensions, Platform } from 'react-native';
import Svg, { Line, Circle, Defs, RadialGradient, Stop, Rect } from 'react-native-svg';
import Animated, { 
  useAnimatedProps, 
  useSharedValue, 
  withRepeat, 
  withTiming, 
  Easing,
  interpolate,
  type SharedValue
} from 'react-native-reanimated';



const AnimatedCircle = Animated.createAnimatedComponent(Circle);
const AnimatedLine = Animated.createAnimatedComponent(Line);

interface Node {
  id: number;
  x: number;
  y: number;
  targetX: number;
  targetY: number;
}

interface MeshBackgroundProps {
  confirmedCount: number;
  unconfirmedCount: number;
}

const isWeb = Platform.OS === 'web';
const NODE_COUNT = isWeb ? 20 : 8; // Web is more powerful than mobile
const NOISE_COUNT = isWeb ? 50 : 20;

export const MeshBackground: React.FC<MeshBackgroundProps> = ({ confirmedCount, unconfirmedCount }) => {
  const { width, height } = useWindowDimensions();

  // Generate random base nodes for the mesh
  const nodes = useMemo(() => {
    return Array.from({ length: NODE_COUNT }).map((_, i) => ({
      id: i,
      x: Math.random() * (width || 500),
      y: Math.random() * (height || 500),
      offsetX: (Math.random() - 0.5) * 100,
      offsetY: (Math.random() - 0.5) * 100,
    }));
  }, [width, height]);

  // Stable noise particles to prevent flicker
  const noiseParticles = useMemo(() => {
    return Array.from({ length: NOISE_COUNT }).map((_, i) => ({
      id: i,
      x: Math.random() * width,
      y: Math.random() * height,
      opacity: Math.random() * 0.3,
    }));
  }, [width, height]);

  // Shared values for animation
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withRepeat(
      withTiming(1, { duration: 15000, easing: Easing.inOut(Easing.sin) }),
      -1,
      true
    );
  }, []);

  // Map unconfirmed count to "noise" intensity (opacity of small dots)
  const noiseOpacity = Math.min(unconfirmedCount * 0.05, 0.3);
  
  // Map confirmed count to line visibility
  const lineOpacity = Math.min(confirmedCount * 0.1, 0.8);

  return (
    <View style={styles.container}>
      <Svg width={width} height={height} style={StyleSheet.absoluteFill}>
        <Defs>
          <RadialGradient id="grad" cx="50%" cy="50%" rx="50%" ry="50%">
            <Stop offset="0%" stopColor="#0891B2" stopOpacity="0.3" />
            <Stop offset="100%" stopColor="#000000" stopOpacity="0" />
          </RadialGradient>
        </Defs>

        {/* Pure Black Background */}
        <Rect width="100%" height="100%" fill="#000000" />

        {/* Subtle Background Glow */}
        <Circle cx={width / 2} cy={height / 2} r={width * 0.8} fill="url(#grad)" />

        {/* Unconfirmed "Noise" Particles - Stable Random Static Dots */}
        {unconfirmedCount > 0 && noiseParticles.map((particle) => (
          <Circle
            key={`noise-${particle.id}`}
            cx={particle.x}
            cy={particle.y}
            r={1}
            fill="#22D3EE"
            fillOpacity={particle.opacity * noiseOpacity}
          />
        ))}

        {/* Confirmed Geometric Mesh */}
        {nodes.map((node, i) => {
          const neighbors = nodes
            .filter(n => n.id !== node.id)
            .sort((a, b) => {
              const d1 = Math.pow(a.x - node.x, 2) + Math.pow(a.y - node.y, 2);
              const d2 = Math.pow(b.x - node.x, 2) + Math.pow(b.y - node.y, 2);
              return d1 - d2;
            })
            .slice(0, 2);
          return (
            <MeshNode 
              key={`node-group-${i}`}
              node={node as any}
              neighbors={neighbors as any}
              progress={progress}
              lineOpacity={lineOpacity}
              confirmedCount={confirmedCount}
            />
          );
        })}
      </Svg>
    </View>
  );
};

interface MeshNodeProps {
  node: Node & { offsetX: number; offsetY: number };
  neighbors: (Node & { offsetX: number; offsetY: number })[];
  progress: SharedValue<number>;
  lineOpacity: number;
  confirmedCount: number;
}

const MeshNode: React.FC<MeshNodeProps> = ({ node, neighbors, progress, lineOpacity, confirmedCount }) => {
  const animatedProps = useAnimatedProps(() => {
    const currentX = node.x + Math.sin(progress.value * Math.PI * 2) * node.offsetX;
    const currentY = node.y + Math.cos(progress.value * Math.PI * 2) * node.offsetY;
    return {
      cx: currentX,
      cy: currentY,
    };
  });

  return (
    <React.Fragment>
      {confirmedCount > 0 && neighbors.map((neighbor, ni) => {
        const neighborAnimatedProps = useAnimatedProps(() => {
          const currentX1 = node.x + Math.sin(progress.value * Math.PI * 2) * node.offsetX;
          const currentY1 = node.y + Math.cos(progress.value * Math.PI * 2) * node.offsetY;
          const currentX2 = neighbor.x + Math.sin(progress.value * Math.PI * 2) * neighbor.offsetX;
          const currentY2 = neighbor.y + Math.cos(progress.value * Math.PI * 2) * neighbor.offsetY;
          return {
            x1: currentX1,
            y1: currentY1,
            x2: currentX2,
            y2: currentY2,
          };
        });

        return (
          <AnimatedLine
            key={`line-${node.id}-${ni}`}
            animatedProps={neighborAnimatedProps}
            stroke="#22D3EE"
            strokeWidth="0.8"
            strokeOpacity={lineOpacity * 0.4}
          />
        );
      })}

      <AnimatedCircle
        animatedProps={animatedProps}
        r="2"
        fill="#22D3EE"
        fillOpacity={lineOpacity + 0.4}
      />
    </React.Fragment>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#000000',
  },
});
